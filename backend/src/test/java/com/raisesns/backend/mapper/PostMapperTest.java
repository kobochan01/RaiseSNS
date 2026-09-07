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
    void insertPersistsImageUrl() {
        Post post = Post.builder()
                .userId(authorId)
                .body("画像付き投稿")
                .imageUrl("https://example-bucket.s3.ap-northeast-1.amazonaws.com/posts/a.png")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        postMapper.insert(post);

        Post found = postMapper.findById(post.getId()).orElseThrow();
        assertThat(found.getImageUrl()).isEqualTo("https://example-bucket.s3.ap-northeast-1.amazonaws.com/posts/a.png");
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

    @Test
    void findByUserIdReturnsOnlyThatUsersPosts() {
        LocalDateTime now = LocalDateTime.now();
        User otherAuthor = User.builder()
                .username("otherauthor" + System.nanoTime())
                .email("otherauthor" + System.nanoTime() + "@example.com")
                .passwordHash("hashed-password")
                .displayName("別の投稿者")
                .createdAt(now)
                .updatedAt(now)
                .build();
        userMapper.insert(otherAuthor);

        Post mine = newPost("自分の投稿");
        postMapper.insert(mine);
        Post others = Post.builder().userId(otherAuthor.getId()).body("他人の投稿").createdAt(now).updatedAt(now).build();
        postMapper.insert(others);

        List<PostFeedRow> rows = postMapper.findByUserId(authorId, null, 10);

        assertThat(rows).extracting(PostFeedRow::getId).containsExactly(mine.getId());
    }

    @Test
    void findByUserIdWithCursorReturnsOnlyOlderPosts() {
        Post first = newPost("1件目");
        postMapper.insert(first);
        Post second = newPost("2件目");
        postMapper.insert(second);

        List<PostFeedRow> rows = postMapper.findByUserId(authorId, second.getId(), 10);

        assertThat(rows).extracting(PostFeedRow::getId).containsExactly(first.getId());
    }

    @Test
    void findByUserIdRespectsLimit() {
        postMapper.insert(newPost("1件目"));
        postMapper.insert(newPost("2件目"));

        List<PostFeedRow> rows = postMapper.findByUserId(authorId, null, 1);

        assertThat(rows).hasSize(1);
    }
}
