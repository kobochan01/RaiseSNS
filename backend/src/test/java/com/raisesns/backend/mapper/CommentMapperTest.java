package com.raisesns.backend.mapper;

import com.raisesns.backend.entity.Comment;
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
import static org.assertj.core.api.Assertions.tuple;

@Transactional
class CommentMapperTest extends AbstractIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    CommentMapper commentMapper;

    @Autowired
    PostMapper postMapper;

    @Autowired
    UserMapper userMapper;

    private Long authorId;
    private Long commenterId;
    private Long postId;

    @BeforeEach
    void setUp() {
        authorId = insertUser("author");
        commenterId = insertUser("commenter");

        LocalDateTime now = LocalDateTime.now();
        Post post = Post.builder().userId(authorId).body("コメントされる投稿").createdAt(now).updatedAt(now).build();
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

    private Comment newComment(Long parentCommentId, String body) {
        return Comment.builder()
                .postId(postId)
                .userId(commenterId)
                .parentCommentId(parentCommentId)
                .body(body)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void insertAssignsGeneratedId() {
        Comment comment = newComment(null, "はじめてのコメント");

        commentMapper.insert(comment);

        assertThat(comment.getId()).isNotNull();
    }

    @Test
    void findByIdReturnsMatchingComment() {
        Comment comment = newComment(null, "見つかるコメント");
        commentMapper.insert(comment);

        Optional<Comment> found = commentMapper.findById(comment.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getBody()).isEqualTo("見つかるコメント");
    }

    @Test
    void deleteByIdRemovesComment() {
        Comment comment = newComment(null, "削除されるコメント");
        commentMapper.insert(comment);

        commentMapper.deleteById(comment.getId());

        assertThat(commentMapper.findById(comment.getId())).isEmpty();
    }

    @Test
    void deletingParentCommentCascadesToReplies() {
        Comment parent = newComment(null, "親コメント");
        commentMapper.insert(parent);
        Comment reply = newComment(parent.getId(), "返信コメント");
        commentMapper.insert(reply);

        commentMapper.deleteById(parent.getId());

        assertThat(commentMapper.findById(parent.getId())).isEmpty();
        assertThat(commentMapper.findById(reply.getId())).isEmpty();
    }

    @Test
    void findByPostIdReturnsCommentsWithAuthorInfoInAscendingOrder() {
        Comment first = newComment(null, "1件目");
        commentMapper.insert(first);
        Comment second = newComment(null, "2件目");
        commentMapper.insert(second);

        List<CommentRow> rows = commentMapper.findByPostId(postId);

        assertThat(rows).extracting(CommentRow::getId).containsExactly(first.getId(), second.getId());
        assertThat(rows.get(0).getAuthorId()).isEqualTo(commenterId);
        assertThat(rows.get(0).getAuthorDisplayName()).isEqualTo("commenter");
    }

    @Test
    void findByPostIdIncludesRepliesWithParentCommentId() {
        Comment parent = newComment(null, "親コメント");
        commentMapper.insert(parent);
        Comment reply = newComment(parent.getId(), "返信コメント");
        commentMapper.insert(reply);

        List<CommentRow> rows = commentMapper.findByPostId(postId);

        CommentRow replyRow = rows.stream().filter(r -> r.getId().equals(reply.getId())).findFirst().orElseThrow();
        assertThat(replyRow.getParentCommentId()).isEqualTo(parent.getId());
    }

    @Test
    void countByPostIdReturnsNumberOfComments() {
        commentMapper.insert(newComment(null, "1件目"));
        commentMapper.insert(newComment(null, "2件目"));

        assertThat(commentMapper.countByPostId(postId)).isEqualTo(2);
    }

    @Test
    void countsByPostIdsAggregatesPerPost() {
        LocalDateTime now = LocalDateTime.now();
        Post secondPost = Post.builder().userId(authorId).body("2件目の投稿").createdAt(now).updatedAt(now).build();
        postMapper.insert(secondPost);

        commentMapper.insert(newComment(null, "1件目投稿へのコメント"));
        Comment onSecondPost = Comment.builder()
                .postId(secondPost.getId()).userId(commenterId).body("2件目投稿へのコメント").createdAt(now).build();
        commentMapper.insert(onSecondPost);

        List<PostCountRow> counts = commentMapper.countsByPostIds(List.of(postId, secondPost.getId()));

        assertThat(counts).extracting(PostCountRow::getPostId, PostCountRow::getCount)
                .containsExactlyInAnyOrder(tuple(postId, 1), tuple(secondPost.getId(), 1));
    }
}
