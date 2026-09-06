package com.raisesns.backend.mapper;

import com.raisesns.backend.entity.Post;
import com.raisesns.backend.entity.User;
import com.raisesns.backend.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@Transactional
class LikeMapperTest extends AbstractIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    LikeMapper likeMapper;

    @Autowired
    PostMapper postMapper;

    @Autowired
    UserMapper userMapper;

    private Long authorId;
    private Long likerId;
    private Long postId;

    @BeforeEach
    void setUp() {
        authorId = insertUser("author");
        likerId = insertUser("liker");

        LocalDateTime now = LocalDateTime.now();
        Post post = Post.builder().userId(authorId).body("いいねされる投稿").createdAt(now).updatedAt(now).build();
        postMapper.insert(post);
        postId = post.getId();
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
    void insertIfAbsentCreatesLike() {
        likeMapper.insertIfAbsent(postId, likerId, LocalDateTime.now());

        assertThat(likeMapper.existsByPostIdAndUserId(postId, likerId)).isTrue();
        assertThat(likeMapper.countByPostId(postId)).isEqualTo(1);
    }

    @Test
    void insertIfAbsentIsIdempotent() {
        likeMapper.insertIfAbsent(postId, likerId, LocalDateTime.now());
        likeMapper.insertIfAbsent(postId, likerId, LocalDateTime.now());

        assertThat(likeMapper.countByPostId(postId)).isEqualTo(1);
    }

    @Test
    void deleteByPostIdAndUserIdRemovesLike() {
        likeMapper.insertIfAbsent(postId, likerId, LocalDateTime.now());

        likeMapper.deleteByPostIdAndUserId(postId, likerId);

        assertThat(likeMapper.existsByPostIdAndUserId(postId, likerId)).isFalse();
        assertThat(likeMapper.countByPostId(postId)).isEqualTo(0);
    }

    @Test
    void deleteByPostIdAndUserIdIsIdempotentWhenNotLiked() {
        likeMapper.deleteByPostIdAndUserId(postId, likerId);

        assertThat(likeMapper.countByPostId(postId)).isEqualTo(0);
    }

    @Test
    void existsByPostIdAndUserIdReturnsFalseWhenNotLiked() {
        assertThat(likeMapper.existsByPostIdAndUserId(postId, likerId)).isFalse();
    }

    @Test
    void countsByPostIdsAggregatesPerPost() {
        LocalDateTime now = LocalDateTime.now();
        Post secondPost = Post.builder().userId(authorId).body("2件目の投稿").createdAt(now).updatedAt(now).build();
        postMapper.insert(secondPost);

        likeMapper.insertIfAbsent(postId, likerId, now);
        likeMapper.insertIfAbsent(secondPost.getId(), likerId, now);
        likeMapper.insertIfAbsent(secondPost.getId(), authorId, now);

        List<PostCountRow> counts = likeMapper.countsByPostIds(List.of(postId, secondPost.getId()));

        assertThat(counts).extracting(PostCountRow::getPostId, PostCountRow::getCount)
                .containsExactlyInAnyOrder(tuple(postId, 1), tuple(secondPost.getId(), 2));
    }

    @Test
    void findLikedPostIdsReturnsOnlyPostsLikedByUser() {
        LocalDateTime now = LocalDateTime.now();
        Post secondPost = Post.builder().userId(authorId).body("2件目の投稿").createdAt(now).updatedAt(now).build();
        postMapper.insert(secondPost);

        likeMapper.insertIfAbsent(postId, likerId, now);

        List<Long> likedPostIds = likeMapper.findLikedPostIds(likerId, List.of(postId, secondPost.getId()));

        assertThat(likedPostIds).containsExactly(postId);
    }
}
