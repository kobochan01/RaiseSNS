package com.raisesns.backend.service;

import com.raisesns.backend.dto.request.LoginRequest;
import com.raisesns.backend.dto.request.RegisterRequest;
import com.raisesns.backend.dto.response.RegisterResponse;
import com.raisesns.backend.entity.RefreshToken;
import com.raisesns.backend.entity.User;
import com.raisesns.backend.exception.DuplicateEmailException;
import com.raisesns.backend.exception.DuplicateUsernameException;
import com.raisesns.backend.exception.InvalidCredentialsException;
import com.raisesns.backend.exception.InvalidRefreshTokenException;
import com.raisesns.backend.mapper.RefreshTokenMapper;
import com.raisesns.backend.mapper.UserMapper;
import com.raisesns.backend.security.JwtTokenProvider;
import com.raisesns.backend.security.RefreshTokenProperties;
import com.raisesns.backend.security.RefreshTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private final UserMapper userMapper = mock(UserMapper.class);
    private final RefreshTokenMapper refreshTokenMapper = mock(RefreshTokenMapper.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
    private final RefreshTokenProvider refreshTokenProvider = mock(RefreshTokenProvider.class);
    private final RefreshTokenProperties refreshTokenProperties = new RefreshTokenProperties(7);
    private final AuthService authService = new AuthService(
            userMapper, refreshTokenMapper, passwordEncoder, jwtTokenProvider, refreshTokenProvider,
            refreshTokenProperties);

    @BeforeEach
    void setUp() {
        doAnswer(invocation -> {
            User user = invocation.getArgument(0);
            ReflectionTestUtils.setField(user, "id", 100L);
            return null;
        }).when(userMapper).insert(any(User.class));
    }

    @Test
    void registerSavesHashedPasswordAndReturnsResponse() {
        when(userMapper.existsByUsername("newuser")).thenReturn(false);
        when(userMapper.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("raw-password")).thenReturn("hashed-password");

        RegisterRequest request = new RegisterRequest("newuser", "new@example.com", "raw-password", "表示名");
        RegisterResponse response = authService.register(request);

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.username()).isEqualTo("newuser");
        assertThat(response.displayName()).isEqualTo("表示名");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("hashed-password");
    }

    @Test
    void registerThrowsWhenUsernameAlreadyExists() {
        when(userMapper.existsByUsername("taken")).thenReturn(true);

        RegisterRequest request = new RegisterRequest("taken", "new@example.com", "raw-password", "表示名");

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateUsernameException.class);
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void registerThrowsWhenEmailAlreadyExists() {
        when(userMapper.existsByUsername("newuser")).thenReturn(false);
        when(userMapper.existsByEmail("taken@example.com")).thenReturn(true);

        RegisterRequest request = new RegisterRequest("newuser", "taken@example.com", "raw-password", "表示名");

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateEmailException.class);
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void loginReturnsAccessAndRefreshTokenAndUserInfoOnSuccess() {
        User user = User.builder()
                .id(5L)
                .username("bob")
                .email("bob@example.com")
                .passwordHash("hashed-password")
                .displayName("ボブ")
                .avatarUrl("http://example.com/avatar.png")
                .build();
        when(userMapper.findByEmail("bob@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("raw-password", "hashed-password")).thenReturn(true);
        when(jwtTokenProvider.generateToken(5L, "bob@example.com")).thenReturn("jwt-token");
        when(refreshTokenProvider.generateRawToken()).thenReturn("raw-refresh-token");
        when(refreshTokenProvider.hash("raw-refresh-token")).thenReturn("hashed-refresh-token");

        AuthService.LoginResult result = authService.login(new LoginRequest("bob@example.com", "raw-password"));

        assertThat(result.accessToken()).isEqualTo("jwt-token");
        assertThat(result.refreshToken()).isEqualTo("raw-refresh-token");
        assertThat(result.loginResponse().id()).isEqualTo(5L);
        assertThat(result.loginResponse().username()).isEqualTo("bob");
        assertThat(result.loginResponse().displayName()).isEqualTo("ボブ");
        assertThat(result.loginResponse().avatarUrl()).isEqualTo("http://example.com/avatar.png");

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenMapper).insert(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(5L);
        assertThat(captor.getValue().getTokenHash()).isEqualTo("hashed-refresh-token");
    }

    @Test
    void loginThrowsInvalidCredentialsWhenEmailNotFound() {
        when(userMapper.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("nobody@example.com", "raw-password")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void loginThrowsInvalidCredentialsWhenPasswordDoesNotMatch() {
        User user = User.builder()
                .id(5L)
                .username("bob")
                .email("bob@example.com")
                .passwordHash("hashed-password")
                .displayName("ボブ")
                .build();
        when(userMapper.findByEmail("bob@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("bob@example.com", "wrong-password")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void refreshRotatesTokenAndReturnsNewTokensWhenValid() {
        RefreshToken existing = RefreshToken.builder()
                .id(1L)
                .userId(5L)
                .tokenHash("hashed-old-token")
                .expiresAt(LocalDateTime.now().plusDays(1))
                .createdAt(LocalDateTime.now())
                .build();
        User user = User.builder()
                .id(5L)
                .username("bob")
                .email("bob@example.com")
                .displayName("ボブ")
                .build();
        when(refreshTokenProvider.hash("raw-old-token")).thenReturn("hashed-old-token");
        when(refreshTokenMapper.findValidByTokenHash("hashed-old-token")).thenReturn(Optional.of(existing));
        when(userMapper.findById(5L)).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateToken(5L, "bob@example.com")).thenReturn("new-jwt-token");
        when(refreshTokenProvider.generateRawToken()).thenReturn("raw-new-token");
        when(refreshTokenProvider.hash("raw-new-token")).thenReturn("hashed-new-token");

        AuthService.RefreshResult result = authService.refresh("raw-old-token");

        assertThat(result.accessToken()).isEqualTo("new-jwt-token");
        assertThat(result.refreshToken()).isEqualTo("raw-new-token");
        assertThat(result.loginResponse().displayName()).isEqualTo("ボブ");
        verify(refreshTokenMapper).revokeByTokenHash("hashed-old-token");
        verify(refreshTokenMapper).insert(any(RefreshToken.class));
    }

    @Test
    void refreshThrowsInvalidRefreshTokenWhenNotFound() {
        when(refreshTokenProvider.hash("bad-token")).thenReturn("hashed-bad-token");
        when(refreshTokenMapper.findValidByTokenHash("hashed-bad-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh("bad-token"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void logoutRevokesTokenWhenPresent() {
        when(refreshTokenProvider.hash("raw-token")).thenReturn("hashed-token");

        authService.logout("raw-token");

        verify(refreshTokenMapper).revokeByTokenHash("hashed-token");
    }

    @Test
    void logoutDoesNothingWhenTokenBlank() {
        authService.logout(" ");

        verify(refreshTokenMapper, never()).revokeByTokenHash(any());
    }
}
