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

class CommentsTableMigrationTest extends AbstractIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void commentsTableHasExpectedColumns() {
        List<Map<String, Object>> columns = jdbcTemplate.queryForList(
                "SELECT column_name, is_nullable FROM information_schema.columns "
                        + "WHERE table_name = 'comments' ORDER BY ordinal_position");

        assertThat(columns).extracting(c -> c.get("column_name"))
                .containsExactly("id", "post_id", "user_id", "parent_comment_id", "body", "created_at");

        Map<String, Object> parentCommentId = columns.stream()
                .filter(c -> "parent_comment_id".equals(c.get("column_name")))
                .findFirst().orElseThrow();
        assertThat(parentCommentId.get("is_nullable")).isEqualTo("YES");

        Map<String, Object> body = columns.stream()
                .filter(c -> "body".equals(c.get("column_name")))
                .findFirst().orElseThrow();
        assertThat(body.get("is_nullable")).isEqualTo("NO");
    }

    @Test
    void commentsTableHasForeignKeys() {
        List<String> constraints = jdbcTemplate.queryForList(
                "SELECT constraint_name FROM information_schema.table_constraints "
                        + "WHERE table_name = 'comments' AND constraint_type = 'FOREIGN KEY'",
                String.class);
        assertThat(constraints).contains(
                "fk_comments_post_id", "fk_comments_user_id", "fk_comments_parent_comment_id");
    }
}
