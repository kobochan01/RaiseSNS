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

class FollowsTableMigrationTest extends AbstractIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void followsTableHasExpectedColumns() {
        List<Map<String, Object>> columns = jdbcTemplate.queryForList(
                "SELECT column_name, is_nullable FROM information_schema.columns "
                        + "WHERE table_name = 'follows' ORDER BY ordinal_position");

        assertThat(columns).extracting(c -> c.get("column_name"))
                .containsExactly("follower_id", "followee_id", "created_at");

        Map<String, Object> createdAt = columns.stream()
                .filter(c -> "created_at".equals(c.get("column_name")))
                .findFirst().orElseThrow();
        assertThat(createdAt.get("is_nullable")).isEqualTo("NO");
    }

    @Test
    void followsTableHasCompositePrimaryKey() {
        List<String> pkColumns = jdbcTemplate.queryForList(
                "SELECT kcu.column_name FROM information_schema.table_constraints tc "
                        + "JOIN information_schema.key_column_usage kcu ON kcu.constraint_name = tc.constraint_name "
                        + "WHERE tc.table_name = 'follows' AND tc.constraint_type = 'PRIMARY KEY' "
                        + "ORDER BY kcu.ordinal_position",
                String.class);

        assertThat(pkColumns).containsExactly("follower_id", "followee_id");
    }

    @Test
    void followsTableHasForeignKeysAndCheckConstraint() {
        List<String> foreignKeys = jdbcTemplate.queryForList(
                "SELECT constraint_name FROM information_schema.table_constraints "
                        + "WHERE table_name = 'follows' AND constraint_type = 'FOREIGN KEY'",
                String.class);
        assertThat(foreignKeys).contains("fk_follows_follower_id", "fk_follows_followee_id");

        List<String> checkConstraints = jdbcTemplate.queryForList(
                "SELECT constraint_name FROM information_schema.table_constraints "
                        + "WHERE table_name = 'follows' AND constraint_type = 'CHECK' "
                        + "AND constraint_name = 'chk_follows_no_self_follow'",
                String.class);
        assertThat(checkConstraints).contains("chk_follows_no_self_follow");
    }
}
