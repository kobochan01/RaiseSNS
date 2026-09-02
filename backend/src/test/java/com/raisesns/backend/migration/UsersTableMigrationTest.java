package com.raisesns.backend.migration;

import com.raisesns.backend.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class UsersTableMigrationTest extends AbstractIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void usersTableHasExpectedColumns() {
        List<Map<String, Object>> columns = jdbcTemplate.queryForList(
                "SELECT column_name, is_nullable FROM information_schema.columns "
                        + "WHERE table_name = 'users' ORDER BY ordinal_position");

        assertThat(columns).extracting(c -> c.get("column_name"))
                .containsExactly("id", "username", "email", "password_hash", "display_name",
                        "bio", "avatar_url", "created_at", "updated_at");

        Map<String, Object> bio = columns.stream()
                .filter(c -> "bio".equals(c.get("column_name")))
                .findFirst().orElseThrow();
        assertThat(bio.get("is_nullable")).isEqualTo("YES");

        Map<String, Object> username = columns.stream()
                .filter(c -> "username".equals(c.get("column_name")))
                .findFirst().orElseThrow();
        assertThat(username.get("is_nullable")).isEqualTo("NO");
    }

    @Test
    void usernameAndEmailHaveUniqueConstraints() {
        List<String> uniqueConstraints = jdbcTemplate.queryForList(
                "SELECT constraint_name FROM information_schema.table_constraints "
                        + "WHERE table_name = 'users' AND constraint_type = 'UNIQUE'",
                String.class);

        assertThat(uniqueConstraints).contains("uq_users_username", "uq_users_email");
    }
}
