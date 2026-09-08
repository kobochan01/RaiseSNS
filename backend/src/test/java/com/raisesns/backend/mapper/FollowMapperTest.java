package com.raisesns.backend.mapper;

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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class FollowMapperTest extends AbstractIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    FollowMapper followMapper;

    @Autowired
    UserMapper userMapper;

    private Long userAId;
    private Long userBId;

    @BeforeEach
    void setUp() {
        userAId = insertUser("followerA");
        userBId = insertUser("followeeB");
    }

    private Long insertUser(String prefix) {
        LocalDateTime now = LocalDateTime.now();
        User user = User.builder()
                .username(prefix + System.nanoTime())
                .email(prefix + System.nanoTime() + "@example.com")
                .passwordHash("hashed-password")
                .displayName(prefix)
                .createdAt(now)
                .updatedAt(now)
                .build();
        userMapper.insert(user);
        return user.getId();
    }

    @Test
    void insertIfAbsentCreatesFollow() {
        followMapper.insertIfAbsent(userAId, userBId, LocalDateTime.now());

        assertThat(followMapper.existsByFollowerIdAndFolloweeId(userAId, userBId)).isTrue();
        assertThat(followMapper.countByFolloweeId(userBId)).isEqualTo(1);
        assertThat(followMapper.countByFollowerId(userAId)).isEqualTo(1);
    }

    @Test
    void insertIfAbsentIsIdempotent() {
        followMapper.insertIfAbsent(userAId, userBId, LocalDateTime.now());
        followMapper.insertIfAbsent(userAId, userBId, LocalDateTime.now());

        assertThat(followMapper.countByFolloweeId(userBId)).isEqualTo(1);
    }

    @Test
    void insertViolatesCheckConstraintForSelfFollow() {
        assertThatThrownBy(() -> followMapper.insertIfAbsent(userAId, userAId, LocalDateTime.now()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deleteByFollowerIdAndFolloweeIdRemovesFollow() {
        followMapper.insertIfAbsent(userAId, userBId, LocalDateTime.now());

        followMapper.deleteByFollowerIdAndFolloweeId(userAId, userBId);

        assertThat(followMapper.existsByFollowerIdAndFolloweeId(userAId, userBId)).isFalse();
        assertThat(followMapper.countByFolloweeId(userBId)).isZero();
    }

    @Test
    void deleteByFollowerIdAndFolloweeIdIsIdempotentWhenNotFollowing() {
        followMapper.deleteByFollowerIdAndFolloweeId(userAId, userBId);

        assertThat(followMapper.countByFolloweeId(userBId)).isZero();
    }

    @Test
    void existsByFollowerIdAndFolloweeIdReturnsFalseWhenNotFollowing() {
        assertThat(followMapper.existsByFollowerIdAndFolloweeId(userAId, userBId)).isFalse();
    }

    @Test
    void findFollowingReturnsFolloweesWithViewerFollowState() {
        Long userCId = insertUser("followeeC");
        followMapper.insertIfAbsent(userAId, userBId, LocalDateTime.now());
        followMapper.insertIfAbsent(userAId, userCId, LocalDateTime.now());
        followMapper.insertIfAbsent(userCId, userBId, LocalDateTime.now());

        List<FollowUserRow> rows = followMapper.findFollowing(userAId, userCId, 10);

        assertThat(rows).hasSize(2);
        FollowUserRow rowB = rows.stream().filter(r -> r.getId().equals(userBId)).findFirst().orElseThrow();
        assertThat(rowB.isFollowedByMe()).isTrue();
        FollowUserRow rowC = rows.stream().filter(r -> r.getId().equals(userCId)).findFirst().orElseThrow();
        assertThat(rowC.isFollowedByMe()).isFalse();
    }

    @Test
    void findFollowingRespectsLimit() {
        Long userCId = insertUser("followeeC");
        followMapper.insertIfAbsent(userAId, userBId, LocalDateTime.now());
        followMapper.insertIfAbsent(userAId, userCId, LocalDateTime.now());

        List<FollowUserRow> rows = followMapper.findFollowing(userAId, userAId, 1);

        assertThat(rows).hasSize(1);
    }

    @Test
    void findFollowersReturnsFollowersWithViewerFollowState() {
        Long userCId = insertUser("followerC");
        followMapper.insertIfAbsent(userAId, userBId, LocalDateTime.now());
        followMapper.insertIfAbsent(userCId, userBId, LocalDateTime.now());
        followMapper.insertIfAbsent(userAId, userCId, LocalDateTime.now());

        List<FollowUserRow> rows = followMapper.findFollowers(userBId, userAId, 10);

        assertThat(rows).hasSize(2);
        FollowUserRow rowC = rows.stream().filter(r -> r.getId().equals(userCId)).findFirst().orElseThrow();
        assertThat(rowC.isFollowedByMe()).isTrue();
        FollowUserRow rowA = rows.stream().filter(r -> r.getId().equals(userAId)).findFirst().orElseThrow();
        assertThat(rowA.isFollowedByMe()).isFalse();
    }

    @Test
    void findFollowersRespectsLimit() {
        Long userCId = insertUser("followerC");
        followMapper.insertIfAbsent(userAId, userBId, LocalDateTime.now());
        followMapper.insertIfAbsent(userCId, userBId, LocalDateTime.now());

        List<FollowUserRow> rows = followMapper.findFollowers(userBId, userAId, 1);

        assertThat(rows).hasSize(1);
    }
}
