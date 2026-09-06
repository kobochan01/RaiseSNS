package com.raisesns.backend.service;

import com.raisesns.backend.dto.request.CreateCommentRequest;
import com.raisesns.backend.dto.response.CommentResponse;
import com.raisesns.backend.entity.Comment;
import com.raisesns.backend.entity.Post;
import com.raisesns.backend.entity.User;
import com.raisesns.backend.exception.CommentAccessDeniedException;
import com.raisesns.backend.exception.CommentNotFoundException;
import com.raisesns.backend.exception.InvalidCommentParentException;
import com.raisesns.backend.exception.PostNotFoundException;
import com.raisesns.backend.mapper.CommentMapper;
import com.raisesns.backend.mapper.CommentRow;
import com.raisesns.backend.mapper.PostMapper;
import com.raisesns.backend.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommentServiceTest {

    private final CommentMapper commentMapper = mock(CommentMapper.class);
    private final PostMapper postMapper = mock(PostMapper.class);
    private final UserMapper userMapper = mock(UserMapper.class);
    private final CommentService commentService = new CommentService(commentMapper, postMapper, userMapper);

    private Post existingPost(Long id) {
        LocalDateTime now = LocalDateTime.now();
        return Post.builder().id(id).userId(1L).body("投稿").createdAt(now).updatedAt(now).build();
    }

    private User author(Long id) {
        return User.builder().id(id).username("commenter").displayName("コメント投稿者").avatarUrl(null).build();
    }

    private Comment existingComment(Long id, Long postId, Long userId) {
        return Comment.builder().id(id).postId(postId).userId(userId).body("本文").createdAt(LocalDateTime.now()).build();
    }

    @BeforeEach
    void setUp() {
        doAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            ReflectionTestUtils.setField(comment, "id", 100L);
            return null;
        }).when(commentMapper).insert(any(Comment.class));
    }

    @Test
    void createTopLevelCommentSucceeds() {
        when(postMapper.findById(10L)).thenReturn(Optional.of(existingPost(10L)));
        when(userMapper.findById(5L)).thenReturn(Optional.of(author(5L)));

        CommentResponse response = commentService.create(5L, 10L, new CreateCommentRequest("いいですね", null));

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.body()).isEqualTo("いいですね");
        assertThat(response.parentCommentId()).isNull();
        assertThat(response.replies()).isEmpty();
    }

    @Test
    void createThrowsPostNotFoundWhenPostDoesNotExist() {
        when(postMapper.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.create(5L, 10L, new CreateCommentRequest("本文", null)))
                .isInstanceOf(PostNotFoundException.class);
        verify(commentMapper, never()).insert(any());
    }

    @Test
    void createReplySucceedsWhenParentBelongsToSamePost() {
        when(postMapper.findById(10L)).thenReturn(Optional.of(existingPost(10L)));
        when(commentMapper.findById(1L)).thenReturn(Optional.of(existingComment(1L, 10L, 2L)));
        when(userMapper.findById(5L)).thenReturn(Optional.of(author(5L)));

        CommentResponse response = commentService.create(5L, 10L, new CreateCommentRequest("返信です", 1L));

        assertThat(response.parentCommentId()).isEqualTo(1L);
    }

    @Test
    void createThrowsCommentNotFoundWhenParentDoesNotExist() {
        when(postMapper.findById(10L)).thenReturn(Optional.of(existingPost(10L)));
        when(commentMapper.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.create(5L, 10L, new CreateCommentRequest("返信です", 1L)))
                .isInstanceOf(CommentNotFoundException.class);
        verify(commentMapper, never()).insert(any());
    }

    @Test
    void createThrowsInvalidCommentParentWhenParentBelongsToDifferentPost() {
        when(postMapper.findById(10L)).thenReturn(Optional.of(existingPost(10L)));
        when(commentMapper.findById(1L)).thenReturn(Optional.of(existingComment(1L, 999L, 2L)));

        assertThatThrownBy(() -> commentService.create(5L, 10L, new CreateCommentRequest("返信です", 1L)))
                .isInstanceOf(InvalidCommentParentException.class);
        verify(commentMapper, never()).insert(any());
    }

    @Test
    void getCommentsBuildsNestedTreeFromFlatRows() {
        when(postMapper.findById(10L)).thenReturn(Optional.of(existingPost(10L)));
        CommentRow parent = row(1L, null, "親コメント");
        CommentRow reply = row(2L, 1L, "返信コメント");
        CommentRow grandchild = row(3L, 2L, "孫コメント");
        when(commentMapper.findByPostId(10L)).thenReturn(List.of(parent, reply, grandchild));

        List<CommentResponse> tree = commentService.getComments(10L);

        assertThat(tree).hasSize(1);
        CommentResponse parentResponse = tree.get(0);
        assertThat(parentResponse.id()).isEqualTo(1L);
        assertThat(parentResponse.replies()).hasSize(1);
        CommentResponse replyResponse = parentResponse.replies().get(0);
        assertThat(replyResponse.id()).isEqualTo(2L);
        assertThat(replyResponse.replies()).hasSize(1);
        assertThat(replyResponse.replies().get(0).id()).isEqualTo(3L);
    }

    @Test
    void getCommentsThrowsPostNotFoundWhenPostDoesNotExist() {
        when(postMapper.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.getComments(10L)).isInstanceOf(PostNotFoundException.class);
    }

    @Test
    void deleteSucceedsWhenRequesterIsOwner() {
        Comment comment = existingComment(1L, 10L, 5L);
        when(commentMapper.findById(1L)).thenReturn(Optional.of(comment));
        when(commentMapper.countByPostId(10L)).thenReturn(3);

        int commentCount = commentService.delete(5L, 1L);

        verify(commentMapper).deleteById(1L);
        assertThat(commentCount).isEqualTo(3);
    }

    @Test
    void deleteThrowsCommentNotFoundWhenCommentDoesNotExist() {
        when(commentMapper.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.delete(5L, 1L)).isInstanceOf(CommentNotFoundException.class);
        verify(commentMapper, never()).deleteById(any());
    }

    @Test
    void deleteThrowsAccessDeniedWhenRequesterIsNotOwner() {
        Comment comment = existingComment(1L, 10L, 999L);
        when(commentMapper.findById(1L)).thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.delete(5L, 1L)).isInstanceOf(CommentAccessDeniedException.class);
        verify(commentMapper, never()).deleteById(any());
    }

    private CommentRow row(Long id, Long parentCommentId, String body) {
        return CommentRow.builder()
                .id(id)
                .postId(10L)
                .userId(5L)
                .parentCommentId(parentCommentId)
                .body(body)
                .createdAt(LocalDateTime.now())
                .authorId(5L)
                .authorUsername("commenter")
                .authorDisplayName("コメント投稿者")
                .build();
    }
}
