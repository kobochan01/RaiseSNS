package com.raisesns.backend.mapper;

import com.raisesns.backend.entity.RefreshToken;
import com.raisesns.backend.entity.User;
import com.raisesns.backend.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class RefreshTokenMapperTest extends AbstractIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    RefreshTokenMapper refreshTokenMapper;

    @Autowired
    UserMapper userMapper;

    private Long userId;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();
        User user = User.builder()
                .username("tokenuser" + System.nanoTime())
                .email("tokenuser" + System.nanoTime() + "@example.com")
                .passwordHash("hashed-password")
                .displayName("トークンユーザー")
                .createdAt(now)
                .updatedAt(now)
                .build();
        userMapper.insert(user);
        userId = user.getId();
    }

    private RefreshToken newToken(String tokenHash, LocalDateTime expiresAt) {
        return RefreshToken.builder()
                .userId(userId)
                .tokenHash(tokenHash)
                .expiresAt(expiresAt)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void insertAssignsGeneratedId() {
        RefreshToken token = newToken("hash-1", LocalDateTime.now().plusDays(7));

        refreshTokenMapper.insert(token);

        assertThat(token.getId()).isNotNull();
    }

    @Test
    void findValidByTokenHashReturnsTokenWhenNotExpiredAndNotRevoked() {
        refreshTokenMapper.insert(newToken("hash-valid", LocalDateTime.now().plusDays(7)));

        Optional<RefreshToken> found = refreshTokenMapper.findValidByTokenHash("hash-valid");

        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo(userId);
    }

    @Test
    void findValidByTokenHashReturnsEmptyWhenExpired() {
        refreshTokenMapper.insert(newToken("hash-expired", LocalDateTime.now().minusMinutes(1)));

        Optional<RefreshToken> found = refreshTokenMapper.findValidByTokenHash("hash-expired");

        assertThat(found).isEmpty();
    }

    @Test
    void findValidByTokenHashReturnsEmptyWhenRevoked() {
        refreshTokenMapper.insert(newToken("hash-revoked", LocalDateTime.now().plusDays(7)));
        refreshTokenMapper.revokeByTokenHash("hash-revoked");

        Optional<RefreshToken> found = refreshTokenMapper.findValidByTokenHash("hash-revoked");

        assertThat(found).isEmpty();
    }

    @Test
    void findValidByTokenHashReturnsEmptyWhenTokenHashDoesNotExist() {
        Optional<RefreshToken> found = refreshTokenMapper.findValidByTokenHash("no-such-hash");

        assertThat(found).isEmpty();
    }

    @Test
    void revokeByTokenHashSetsRevokedAt() {
        refreshTokenMapper.insert(newToken("hash-to-revoke", LocalDateTime.now().plusDays(7)));

        refreshTokenMapper.revokeByTokenHash("hash-to-revoke");

        assertThat(refreshTokenMapper.findValidByTokenHash("hash-to-revoke")).isEmpty();
    }

    @Test
    void revokeByTokenHashIsIdempotentWhenAlreadyRevoked() {
        refreshTokenMapper.insert(newToken("hash-double-revoke", LocalDateTime.now().plusDays(7)));
        refreshTokenMapper.revokeByTokenHash("hash-double-revoke");

        refreshTokenMapper.revokeByTokenHash("hash-double-revoke");

        assertThat(refreshTokenMapper.findValidByTokenHash("hash-double-revoke")).isEmpty();
    }

    @Test
    void insertViolatesUniqueConstraintForDuplicateTokenHash() {
        refreshTokenMapper.insert(newToken("hash-duplicate", LocalDateTime.now().plusDays(7)));

        assertThatThrownBy(() -> refreshTokenMapper.insert(newToken("hash-duplicate", LocalDateTime.now().plusDays(7))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void insertViolatesForeignKeyConstraintWhenUserDoesNotExist() {
        RefreshToken token = RefreshToken.builder()
                .userId(-1L)
                .tokenHash("hash-no-user")
                .expiresAt(LocalDateTime.now().plusDays(7))
                .createdAt(LocalDateTime.now())
                .build();

        assertThatThrownBy(() -> refreshTokenMapper.insert(token))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
