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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

// findFeedはテーブル全体を対象にするため、テストごとに他テストの投稿が混ざらないようトランザクションをロールバックする。
@Transactional
class PostMapperTest extends AbstractIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    PostMapper postMapper;

    @Autowired
    UserMapper userMapper;

    private Long authorId;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();
        User author = User.builder()
                .username("postauthor" + System.nanoTime())
                .email("postauthor" + System.nanoTime() + "@example.com")
                .passwordHash("hashed-password")
                .displayName("投稿者")
                .createdAt(now)
                .updatedAt(now)
                .build();
        userMapper.insert(author);
        authorId = author.getId();
    }

    private Post newPost(String body) {
        LocalDateTime now = LocalDateTime.now();
        return Post.builder()
                .userId(authorId)
                .body(body)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    @Test
    void insertAssignsGeneratedId() {
        Post post = newPost("はじめての投稿");

        postMapper.insert(post);

        assertThat(post.getId()).isNotNull();
    }

    @Test
    void findByIdReturnsMatchingPost() {
        Post post = newPost("見つかる投稿");
        postMapper.insert(post);

        Optional<Post> found = postMapper.findById(post.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getBody()).isEqualTo("見つかる投稿");
        assertThat(found.get().getUserId()).isEqualTo(authorId);
    }

    @Test
    void findByIdReturnsEmptyWhenNotFound() {
        Optional<Post> found = postMapper.findById(-1L);

        assertThat(found).isEmpty();
    }

    @Test
    void updateChangesBodyAndUpdatedAt() {
        Post post = newPost("元の本文");
        postMapper.insert(post);

        LocalDateTime updatedAt = LocalDateTime.now().plusMinutes(1);
        Post updated = Post.builder()
                .id(post.getId())
                .userId(post.getUserId())
                .body("更新後の本文")
                .createdAt(post.getCreatedAt())
                .updatedAt(updatedAt)
                .build();
        postMapper.update(updated);

        Post found = postMapper.findById(post.getId()).orElseThrow();
        assertThat(found.getBody()).isEqualTo("更新後の本文");
    }

    @Test
    void deleteByIdRemovesPost() {
        Post post = newPost("削除される投稿");
        postMapper.insert(post);

        postMapper.deleteById(post.getId());

        assertThat(postMapper.findById(post.getId())).isEmpty();
    }

    @Test
    void findFeedReturnsPostsInDescendingIdOrderWithAuthorInfo() {
        Post first = newPost("1件目");
        postMapper.insert(first);
        Post second = newPost("2件目");
        postMapper.insert(second);
        Post third = newPost("3件目");
        postMapper.insert(third);

        List<PostFeedRow> rows = postMapper.findFeed(null, 10);

        assertThat(rows).extracting(PostFeedRow::getId)
                .containsExactly(third.getId(), second.getId(), first.getId());
        assertThat(rows.get(0).getAuthorId()).isEqualTo(authorId);
        assertThat(rows.get(0).getAuthorDisplayName()).isEqualTo("投稿者");
    }

    @Test
    void findFeedRespectsLimit() {
        postMapper.insert(newPost("1件目"));
        postMapper.insert(newPost("2件目"));
        postMapper.insert(newPost("3件目"));

        List<PostFeedRow> rows = postMapper.findFeed(null, 2);

        assertThat(rows).hasSize(2);
    }

    @Test
    void findFeedWithCursorReturnsOnlyOlderPosts() {
        Post first = newPost("1件目");
        postMapper.insert(first);
        Post second = newPost("2件目");
        postMapper.insert(second);
        Post third = newPost("3件目");
        postMapper.insert(third);

        List<PostFeedRow> rows = postMapper.findFeed(third.getId(), 10);

        assertThat(rows).extracting(PostFeedRow::getId)
                .containsExactly(second.getId(), first.getId());
    }

    @Test
    void findNewerThanReturnsOnlyPostsAfterGivenId() {
        Post first = newPost("1件目");
        postMapper.insert(first);
        Post second = newPost("2件目");
        postMapper.insert(second);
        Post third = newPost("3件目");
        postMapper.insert(third);

        List<PostFeedRow> rows = postMapper.findNewerThan(first.getId(), 10);

        assertThat(rows).extracting(PostFeedRow::getId)
                .containsExactly(third.getId(), second.getId());
    }

    @Test
    void findNewerThanReturnsEmptyWhenNoNewerPosts() {
        Post first = newPost("1件目");
        postMapper.insert(first);

        List<PostFeedRow> rows = postMapper.findNewerThan(first.getId(), 10);

        assertThat(rows).isEmpty();
    }

    @Test
    void findNewerThanRespectsLimit() {
        Post first = newPost("1件目");
        postMapper.insert(first);
        postMapper.insert(newPost("2件目"));
        postMapper.insert(newPost("3件目"));

        List<PostFeedRow> rows = postMapper.findNewerThan(first.getId(), 1);

        assertThat(rows).hasSize(1);
    }
}
