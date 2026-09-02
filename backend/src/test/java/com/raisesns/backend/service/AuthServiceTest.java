package com.raisesns.backend.service;

import com.raisesns.backend.dto.request.LoginRequest;
import com.raisesns.backend.dto.request.RegisterRequest;
import com.raisesns.backend.dto.response.RegisterResponse;
import com.raisesns.backend.entity.User;
import com.raisesns.backend.exception.DuplicateEmailException;
import com.raisesns.backend.exception.DuplicateUsernameException;
import com.raisesns.backend.exception.InvalidCredentialsException;
import com.raisesns.backend.mapper.UserMapper;
import com.raisesns.backend.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private final UserMapper userMapper = mock(UserMapper.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
    private final AuthService authService = new AuthService(userMapper, passwordEncoder, jwtTokenProvider);

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
    void loginReturnsTokenAndUserInfoOnSuccess() {
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

        AuthService.LoginResult result = authService.login(new LoginRequest("bob@example.com", "raw-password"));

        assertThat(result.token()).isEqualTo("jwt-token");
        assertThat(result.loginResponse().id()).isEqualTo(5L);
        assertThat(result.loginResponse().username()).isEqualTo("bob");
        assertThat(result.loginResponse().displayName()).isEqualTo("ボブ");
        assertThat(result.loginResponse().avatarUrl()).isEqualTo("http://example.com/avatar.png");
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
}
