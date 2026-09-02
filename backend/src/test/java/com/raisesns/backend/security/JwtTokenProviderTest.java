package com.raisesns.backend.security;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    private static final String SECRET = "test-secret-key-for-jwt-must-be-at-least-32-bytes-long";

    @Test
    void generatedTokenCanBeParsedBackToTheSameUserId() {
        JwtTokenProvider provider = new JwtTokenProvider(new JwtProperties(SECRET, 60_000));

        String token = provider.generateToken(42L, "user@example.com");

        assertThat(provider.parseUserId(token)).isEqualTo(42L);
        assertThat(provider.validateToken(token)).isTrue();
    }

    @Test
    void expiredTokenFailsValidation() throws InterruptedException {
        JwtTokenProvider provider = new JwtTokenProvider(new JwtProperties(SECRET, 1));

        String token = provider.generateToken(1L, "user@example.com");
        Thread.sleep(50);

        assertThat(provider.validateToken(token)).isFalse();
    }

    @Test
    void tamperedTokenFailsValidation() {
        JwtTokenProvider provider = new JwtTokenProvider(new JwtProperties(SECRET, 60_000));
        String token = provider.generateToken(1L, "user@example.com");
        // 末尾1文字だけの反転はbase64urlのパディング用ビットにしか影響しないことがあり、
        // signature検証をすり抜けてflakyになるため、ペイロード領域である中央の文字を変更する。
        int middle = token.length() / 2;
        char replacement = token.charAt(middle) == 'a' ? 'b' : 'a';
        String tampered = token.substring(0, middle) + replacement + token.substring(middle + 1);

        assertThat(provider.validateToken(tampered)).isFalse();
    }

    @Test
    void parseUserIdThrowsForInvalidToken() {
        JwtTokenProvider provider = new JwtTokenProvider(new JwtProperties(SECRET, 60_000));

        assertThatThrownBy(() -> provider.parseUserId("not-a-valid-token"))
                .isInstanceOf(JwtException.class);
    }
}
