package com.raisesns.backend.mapper;

import com.raisesns.backend.entity.User;
import com.raisesns.backend.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserMapperTest extends AbstractIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    UserMapper userMapper;

    private User newUser(String username, String email) {
        LocalDateTime now = LocalDateTime.now();
        return User.builder()
                .username(username)
                .email(email)
                .passwordHash("hashed-password")
                .displayName("表示名")
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    @Test
    void insertAssignsGeneratedId() {
        User user = newUser("alice", "alice@example.com");

        userMapper.insert(user);

        assertThat(user.getId()).isNotNull();
    }

    @Test
    void existsByUsernameReflectsInsertedRows() {
        assertThat(userMapper.existsByUsername("bob")).isFalse();

        userMapper.insert(newUser("bob", "bob@example.com"));

        assertThat(userMapper.existsByUsername("bob")).isTrue();
    }

    @Test
    void existsByEmailReflectsInsertedRows() {
        assertThat(userMapper.existsByEmail("carol@example.com")).isFalse();

        userMapper.insert(newUser("carol", "carol@example.com"));

        assertThat(userMapper.existsByEmail("carol@example.com")).isTrue();
    }

    @Test
    void findByEmailReturnsMatchingUser() {
        userMapper.insert(newUser("dave", "dave@example.com"));

        Optional<User> found = userMapper.findByEmail("dave@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("dave");
        assertThat(found.get().getPasswordHash()).isEqualTo("hashed-password");
    }

    @Test
    void findByEmailReturnsEmptyWhenNotFound() {
        Optional<User> found = userMapper.findByEmail("nobody@example.com");

        assertThat(found).isEmpty();
    }

    @Test
    void duplicateUsernameViolatesUniqueConstraint() {
        userMapper.insert(newUser("erin", "erin1@example.com"));

        assertThatThrownBy(() -> userMapper.insert(newUser("erin", "erin2@example.com")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void duplicateEmailViolatesUniqueConstraint() {
        userMapper.insert(newUser("frank1", "frank@example.com"));

        assertThatThrownBy(() -> userMapper.insert(newUser("frank2", "frank@example.com")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
