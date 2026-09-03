package com.raisesns.backend.service;

import com.raisesns.backend.dto.request.LoginRequest;
import com.raisesns.backend.dto.request.RegisterRequest;
import com.raisesns.backend.dto.response.LoginResponse;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final UserMapper userMapper;
    private final RefreshTokenMapper refreshTokenMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenProvider refreshTokenProvider;
    private final RefreshTokenProperties refreshTokenProperties;

    public AuthService(UserMapper userMapper, RefreshTokenMapper refreshTokenMapper, PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider, RefreshTokenProvider refreshTokenProvider,
            RefreshTokenProperties refreshTokenProperties) {
        this.userMapper = userMapper;
        this.refreshTokenMapper = refreshTokenMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenProvider = refreshTokenProvider;
        this.refreshTokenProperties = refreshTokenProperties;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (userMapper.existsByUsername(request.username())) {
            throw new DuplicateUsernameException(request.username());
        }
        if (userMapper.existsByEmail(request.email())) {
            throw new DuplicateEmailException(request.email());
        }

        LocalDateTime now = LocalDateTime.now();
        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .displayName(request.displayName())
                .createdAt(now)
                .updatedAt(now)
                .build();
        userMapper.insert(user);

        return new RegisterResponse(user.getId(), user.getUsername(), user.getDisplayName());
    }

    @Transactional
    public LoginResult login(LoginRequest request) {
        User user = userMapper.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        String accessToken = jwtTokenProvider.generateToken(user.getId(), user.getEmail());
        String refreshToken = issueRefreshToken(user.getId());
        LoginResponse loginResponse = new LoginResponse(
                user.getId(), user.getUsername(), user.getDisplayName(), user.getAvatarUrl());
        return new LoginResult(accessToken, refreshToken, loginResponse);
    }

    @Transactional
    public RefreshResult refresh(String rawRefreshToken) {
        String tokenHash = refreshTokenProvider.hash(rawRefreshToken);
        RefreshToken existing = refreshTokenMapper.findValidByTokenHash(tokenHash)
                .orElseThrow(InvalidRefreshTokenException::new);
        refreshTokenMapper.revokeByTokenHash(tokenHash);

        User user = userMapper.findById(existing.getUserId())
                .orElseThrow(InvalidRefreshTokenException::new);

        String accessToken = jwtTokenProvider.generateToken(user.getId(), user.getEmail());
        String refreshToken = issueRefreshToken(user.getId());
        LoginResponse loginResponse = new LoginResponse(
                user.getId(), user.getUsername(), user.getDisplayName(), user.getAvatarUrl());
        return new RefreshResult(accessToken, refreshToken, loginResponse);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        refreshTokenMapper.revokeByTokenHash(refreshTokenProvider.hash(rawRefreshToken));
    }

    private String issueRefreshToken(Long userId) {
        String rawToken = refreshTokenProvider.generateRawToken();
        LocalDateTime now = LocalDateTime.now();
        RefreshToken refreshToken = RefreshToken.builder()
                .userId(userId)
                .tokenHash(refreshTokenProvider.hash(rawToken))
                .expiresAt(now.plusDays(refreshTokenProperties.expirationDays()))
                .createdAt(now)
                .build();
        refreshTokenMapper.insert(refreshToken);
        return rawToken;
    }

    public record LoginResult(String accessToken, String refreshToken, LoginResponse loginResponse) {
    }

    public record RefreshResult(String accessToken, String refreshToken, LoginResponse loginResponse) {
    }
}
