package com.raisesns.backend.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenProviderTest {

    private final RefreshTokenProvider provider = new RefreshTokenProvider();

    @Test
    void generateRawTokenReturnsDifferentValuesEachCall() {
        String first = provider.generateRawToken();
        String second = provider.generateRawToken();

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void generateRawTokenIsUrlSafeBase64WithoutPadding() {
        String token = provider.generateRawToken();

        assertThat(token).matches("^[A-Za-z0-9_-]+$");
    }

    @Test
    void hashIsDeterministicForTheSameInput() {
        String rawToken = provider.generateRawToken();

        assertThat(provider.hash(rawToken)).isEqualTo(provider.hash(rawToken));
    }

    @Test
    void hashDiffersForDifferentInputs() {
        String first = provider.hash(provider.generateRawToken());
        String second = provider.hash(provider.generateRawToken());

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void hashReturnsSha256HexDigest() {
        String hash = provider.hash("some-raw-token");

        assertThat(hash).hasSize(64).matches("^[0-9a-f]+$");
    }
}
