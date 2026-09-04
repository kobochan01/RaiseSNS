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

class PostsTableMigrationTest extends AbstractIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void postsTableHasExpectedColumns() {
        List<Map<String, Object>> columns = jdbcTemplate.queryForList(
                "SELECT column_name, is_nullable FROM information_schema.columns "
                        + "WHERE table_name = 'posts' ORDER BY ordinal_position");

        assertThat(columns).extracting(c -> c.get("column_name"))
                .containsExactly("id", "user_id", "body", "image_url", "created_at", "updated_at");

        Map<String, Object> body = columns.stream()
                .filter(c -> "body".equals(c.get("column_name")))
                .findFirst().orElseThrow();
        assertThat(body.get("is_nullable")).isEqualTo("NO");

        Map<String, Object> imageUrl = columns.stream()
                .filter(c -> "image_url".equals(c.get("column_name")))
                .findFirst().orElseThrow();
        assertThat(imageUrl.get("is_nullable")).isEqualTo("YES");
    }

    @Test
    void postsTableHasForeignKeyToUsers() {
        List<String> foreignKeys = jdbcTemplate.queryForList(
                "SELECT constraint_name FROM information_schema.table_constraints "
                        + "WHERE table_name = 'posts' AND constraint_type = 'FOREIGN KEY'",
                String.class);

        assertThat(foreignKeys).contains("fk_posts_user_id");
    }
}
