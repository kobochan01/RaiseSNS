package com.raisesns.backend.controller;

import com.raisesns.backend.dto.request.LoginRequest;
import com.raisesns.backend.dto.request.RegisterRequest;
import com.raisesns.backend.dto.response.LoginResponse;
import com.raisesns.backend.dto.response.RegisterResponse;
import com.raisesns.backend.exception.InvalidRefreshTokenException;
import com.raisesns.backend.security.CookieProperties;
import com.raisesns.backend.security.JwtProperties;
import com.raisesns.backend.security.RefreshTokenProperties;
import com.raisesns.backend.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtProperties jwtProperties;
    private final RefreshTokenProperties refreshTokenProperties;
    private final CookieProperties cookieProperties;

    public AuthController(AuthService authService, JwtProperties jwtProperties,
            RefreshTokenProperties refreshTokenProperties, CookieProperties cookieProperties) {
        this.authService = authService;
        this.jwtProperties = jwtProperties;
        this.refreshTokenProperties = refreshTokenProperties;
        this.cookieProperties = cookieProperties;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthService.LoginResult result = authService.login(request);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildAccessTokenCookie(result.accessToken()).toString())
                .header(HttpHeaders.SET_COOKIE, buildRefreshTokenCookie(result.refreshToken()).toString())
                .body(result.loginResponse());
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(HttpServletRequest request) {
        String rawRefreshToken = extractCookie(request, cookieProperties.refreshTokenName())
                .orElseThrow(InvalidRefreshTokenException::new);
        AuthService.RefreshResult result = authService.refresh(rawRefreshToken);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildAccessTokenCookie(result.accessToken()).toString())
                .header(HttpHeaders.SET_COOKIE, buildRefreshTokenCookie(result.refreshToken()).toString())
                .body(result.loginResponse());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        extractCookie(request, cookieProperties.refreshTokenName()).ifPresent(authService::logout);

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, buildExpiredCookie(cookieProperties.accessTokenName()).toString())
                .header(HttpHeaders.SET_COOKIE, buildExpiredCookie(cookieProperties.refreshTokenName()).toString())
                .build();
    }

    private ResponseCookie buildAccessTokenCookie(String token) {
        return ResponseCookie.from(cookieProperties.accessTokenName(), token)
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofMillis(jwtProperties.expirationMs()))
                .build();
    }

    private ResponseCookie buildRefreshTokenCookie(String token) {
        return ResponseCookie.from(cookieProperties.refreshTokenName(), token)
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofDays(refreshTokenProperties.expirationDays()))
                .build();
    }

    private ResponseCookie buildExpiredCookie(String name) {
        return ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
    }

    private Optional<String> extractCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(cookie -> name.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }
}
