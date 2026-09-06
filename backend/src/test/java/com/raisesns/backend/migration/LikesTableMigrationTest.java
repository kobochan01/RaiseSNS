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

class LikesTableMigrationTest extends AbstractIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void likesTableHasExpectedColumns() {
        List<Map<String, Object>> columns = jdbcTemplate.queryForList(
                "SELECT column_name, is_nullable FROM information_schema.columns "
                        + "WHERE table_name = 'likes' ORDER BY ordinal_position");

        assertThat(columns).extracting(c -> c.get("column_name"))
                .containsExactly("id", "post_id", "user_id", "created_at");

        Map<String, Object> postId = columns.stream()
                .filter(c -> "post_id".equals(c.get("column_name")))
                .findFirst().orElseThrow();
        assertThat(postId.get("is_nullable")).isEqualTo("NO");
    }

    @Test
    void likesTableHasForeignKeysAndUniqueConstraint() {
        List<String> constraints = jdbcTemplate.queryForList(
                "SELECT constraint_name FROM information_schema.table_constraints "
                        + "WHERE table_name = 'likes' AND constraint_type = 'FOREIGN KEY'",
                String.class);
        assertThat(constraints).contains("fk_likes_post_id", "fk_likes_user_id");

        List<String> uniqueConstraints = jdbcTemplate.queryForList(
                "SELECT constraint_name FROM information_schema.table_constraints "
                        + "WHERE table_name = 'likes' AND constraint_type = 'UNIQUE'",
                String.class);
        assertThat(uniqueConstraints).contains("uq_likes_post_id_user_id");
    }
}
