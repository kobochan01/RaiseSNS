package com.raisesns.backend.service;

import com.raisesns.backend.dto.request.LoginRequest;
import com.raisesns.backend.dto.request.RegisterRequest;
import com.raisesns.backend.dto.response.LoginResponse;
import com.raisesns.backend.dto.response.RegisterResponse;
import com.raisesns.backend.entity.User;
import com.raisesns.backend.exception.DuplicateEmailException;
import com.raisesns.backend.exception.DuplicateUsernameException;
import com.raisesns.backend.exception.InvalidCredentialsException;
import com.raisesns.backend.mapper.UserMapper;
import com.raisesns.backend.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
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

    public LoginResult login(LoginRequest request) {
        User user = userMapper.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail());
        LoginResponse loginResponse = new LoginResponse(
                user.getId(), user.getUsername(), user.getDisplayName(), user.getAvatarUrl());
        return new LoginResult(token, loginResponse);
    }

    public record LoginResult(String token, LoginResponse loginResponse) {
    }
}
