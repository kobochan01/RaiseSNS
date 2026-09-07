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
import java.util.List;
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

    @Test
    void findByUsernameReturnsMatchingUser() {
        userMapper.insert(newUser("grace", "grace@example.com"));

        Optional<User> found = userMapper.findByUsername("grace");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("grace@example.com");
    }

    @Test
    void findByUsernameReturnsEmptyWhenNotFound() {
        Optional<User> found = userMapper.findByUsername("nobody");

        assertThat(found).isEmpty();
    }

    @Test
    void updateProfileChangesDisplayNameAndBio() {
        User user = newUser("heidi", "heidi@example.com");
        userMapper.insert(user);

        LocalDateTime updatedAt = LocalDateTime.now().plusMinutes(1);
        userMapper.updateProfile(user.getId(), "新しい表示名", "新しい自己紹介", updatedAt);

        User found = userMapper.findById(user.getId()).orElseThrow();
        assertThat(found.getDisplayName()).isEqualTo("新しい表示名");
        assertThat(found.getBio()).isEqualTo("新しい自己紹介");
    }

    @Test
    void searchByUsernameReturnsCaseInsensitivePartialMatches() {
        userMapper.insert(newUser("searchxyzAlice", "searchxyz-alice@example.com"));
        userMapper.insert(newUser("searchxyzAlbert", "searchxyz-albert@example.com"));
        userMapper.insert(newUser("unrelateduser1", "unrelated1@example.com"));

        List<User> found = userMapper.searchByUsername("SEARCHXYZAL", 20, 0);

        assertThat(found).extracting(User::getUsername)
                .containsExactlyInAnyOrder("searchxyzAlice", "searchxyzAlbert");
    }

    @Test
    void searchByUsernameRespectsLimitAndOffset() {
        userMapper.insert(newUser("searchpage1", "searchpage1@example.com"));
        userMapper.insert(newUser("searchpage2", "searchpage2@example.com"));
        userMapper.insert(newUser("searchpage3", "searchpage3@example.com"));

        List<User> firstPage = userMapper.searchByUsername("searchpage", 2, 0);
        List<User> secondPage = userMapper.searchByUsername("searchpage", 2, 2);

        assertThat(firstPage).hasSize(2);
        assertThat(secondPage).hasSize(1);
    }

    @Test
    void countByUsernameContainingReturnsMatchingCount() {
        userMapper.insert(newUser("countkeywordone", "countkeywordone@example.com"));
        userMapper.insert(newUser("countkeywordtwo", "countkeywordtwo@example.com"));

        int count = userMapper.countByUsernameContaining("countkeyword");

        assertThat(count).isEqualTo(2);
    }
}
