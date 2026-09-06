package com.raisesns.backend.service;

import com.raisesns.backend.dto.request.CreatePostRequest;
import com.raisesns.backend.dto.request.UpdatePostRequest;
import com.raisesns.backend.dto.response.PostResponse;
import com.raisesns.backend.dto.response.TimelineResponse;
import com.raisesns.backend.entity.Post;
import com.raisesns.backend.entity.User;
import com.raisesns.backend.exception.PostAccessDeniedException;
import com.raisesns.backend.exception.PostNotFoundException;
import com.raisesns.backend.mapper.CommentMapper;
import com.raisesns.backend.mapper.LikeMapper;
import com.raisesns.backend.mapper.PostCountRow;
import com.raisesns.backend.mapper.PostFeedRow;
import com.raisesns.backend.mapper.PostMapper;
import com.raisesns.backend.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PostServiceTest {

    private final PostMapper postMapper = mock(PostMapper.class);
    private final UserMapper userMapper = mock(UserMapper.class);
    private final LikeMapper likeMapper = mock(LikeMapper.class);
    private final CommentMapper commentMapper = mock(CommentMapper.class);
    private final PostService postService = new PostService(postMapper, userMapper, likeMapper, commentMapper);

    private User author(Long id) {
        return User.builder().id(id).username("author").displayName("投稿者").avatarUrl(null).build();
    }

    private Post existingPost(Long id, Long userId, String body) {
        LocalDateTime now = LocalDateTime.now();
        return Post.builder().id(id).userId(userId).body(body).createdAt(now).updatedAt(now).build();
    }

    @BeforeEach
    void setUp() {
        doAnswer(invocation -> {
            Post post = invocation.getArgument(0);
            ReflectionTestUtils.setField(post, "id", 100L);
            return null;
        }).when(postMapper).insert(any(Post.class));
    }

    @Test
    void createSavesPostAndReturnsResponseWithAuthor() {
        when(userMapper.findById(5L)).thenReturn(Optional.of(author(5L)));

        PostResponse response = postService.create(5L, new CreatePostRequest("こんにちは"));

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.body()).isEqualTo("こんにちは");
        assertThat(response.author().id()).isEqualTo(5L);
        assertThat(response.author().displayName()).isEqualTo("投稿者");
        assertThat(response.likeCount()).isZero();
        assertThat(response.commentCount()).isZero();
        assertThat(response.isLikedByMe()).isFalse();

        ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
        verify(postMapper).insert(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(5L);
    }

    @Test
    void updateSucceedsWhenRequesterIsOwner() {
        Post post = existingPost(1L, 5L, "元の本文");
        when(postMapper.findById(1L)).thenReturn(Optional.of(post));
        when(userMapper.findById(5L)).thenReturn(Optional.of(author(5L)));

        PostResponse response = postService.update(5L, 1L, new UpdatePostRequest("更新後の本文"));

        assertThat(response.body()).isEqualTo("更新後の本文");
        verify(postMapper).update(any(Post.class));
    }

    @Test
    void updateThrowsPostNotFoundWhenPostDoesNotExist() {
        when(postMapper.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.update(5L, 1L, new UpdatePostRequest("更新後の本文")))
                .isInstanceOf(PostNotFoundException.class);
        verify(postMapper, never()).update(any(Post.class));
    }

    @Test
    void updateThrowsAccessDeniedWhenRequesterIsNotOwner() {
        Post post = existingPost(1L, 5L, "元の本文");
        when(postMapper.findById(1L)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.update(999L, 1L, new UpdatePostRequest("更新後の本文")))
                .isInstanceOf(PostAccessDeniedException.class);
        verify(postMapper, never()).update(any(Post.class));
    }

    @Test
    void deleteSucceedsWhenRequesterIsOwner() {
        Post post = existingPost(1L, 5L, "削除対象");
        when(postMapper.findById(1L)).thenReturn(Optional.of(post));

        postService.delete(5L, 1L);

        verify(postMapper).deleteById(1L);
    }

    @Test
    void deleteThrowsPostNotFoundWhenPostDoesNotExist() {
        when(postMapper.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.delete(5L, 1L))
                .isInstanceOf(PostNotFoundException.class);
        verify(postMapper, never()).deleteById(any());
    }

    @Test
    void deleteThrowsAccessDeniedWhenRequesterIsNotOwner() {
        Post post = existingPost(1L, 5L, "削除対象");
        when(postMapper.findById(1L)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.delete(999L, 1L))
                .isInstanceOf(PostAccessDeniedException.class);
        verify(postMapper, never()).deleteById(any());
    }

    @Test
    void getTimelineReturnsEmptyListForFollowingScope() {
        TimelineResponse response = postService.getTimeline(5L, "following", null, null);

        assertThat(response.posts()).isEmpty();
        assertThat(response.nextCursor()).isNull();
        verify(postMapper, never()).findFeed(any(), anyInt());
    }

    @Test
    void getTimelineUsesDefaultLimitWhenNotSpecified() {
        when(postMapper.findFeed(null, 21)).thenReturn(List.of());

        postService.getTimeline(5L, "all", null, null);

        verify(postMapper).findFeed(null, 21);
    }

    @Test
    void getTimelineClampsLimitToMaximum() {
        when(postMapper.findFeed(null, 51)).thenReturn(List.of());

        postService.getTimeline(5L, "all", null, 1000);

        verify(postMapper).findFeed(null, 51);
    }

    @Test
    void getTimelineSetsNextCursorWhenMoreResultsExist() {
        List<PostFeedRow> rows = List.of(
                feedRow(3L), feedRow(2L), feedRow(1L));
        when(postMapper.findFeed(null, 3)).thenReturn(rows);

        TimelineResponse response = postService.getTimeline(5L, "all", null, 2);

        assertThat(response.posts()).hasSize(2);
        assertThat(response.posts()).extracting(PostResponse::id).containsExactly(3L, 2L);
        assertThat(response.nextCursor()).isEqualTo(2L);
    }

    @Test
    void getTimelineHasNullNextCursorWhenNoMoreResults() {
        List<PostFeedRow> rows = List.of(feedRow(2L), feedRow(1L));
        when(postMapper.findFeed(null, 3)).thenReturn(rows);

        TimelineResponse response = postService.getTimeline(5L, "all", null, 2);

        assertThat(response.posts()).hasSize(2);
        assertThat(response.nextCursor()).isNull();
    }

    @Test
    void getTimelineFillsLikeAndCommentAggregatesWithoutPerPostQueries() {
        List<PostFeedRow> rows = List.of(feedRow(3L), feedRow(2L));
        when(postMapper.findFeed(null, 21)).thenReturn(rows);
        when(likeMapper.countsByPostIds(List.of(3L, 2L)))
                .thenReturn(List.of(PostCountRow.builder().postId(3L).count(2).build()));
        when(commentMapper.countsByPostIds(List.of(3L, 2L)))
                .thenReturn(List.of(PostCountRow.builder().postId(2L).count(1).build()));
        when(likeMapper.findLikedPostIds(5L, List.of(3L, 2L))).thenReturn(List.of(2L));

        TimelineResponse response = postService.getTimeline(5L, "all", null, null);

        PostResponse post3 = response.posts().stream().filter(p -> p.id().equals(3L)).findFirst().orElseThrow();
        PostResponse post2 = response.posts().stream().filter(p -> p.id().equals(2L)).findFirst().orElseThrow();
        assertThat(post3.likeCount()).isEqualTo(2);
        assertThat(post3.commentCount()).isZero();
        assertThat(post3.isLikedByMe()).isFalse();
        assertThat(post2.likeCount()).isZero();
        assertThat(post2.commentCount()).isEqualTo(1);
        assertThat(post2.isLikedByMe()).isTrue();
        // バッチ集計のため、投稿数に関わらずMapper呼び出しは各1回のみ
        verify(likeMapper, times(1)).countsByPostIds(any());
        verify(commentMapper, times(1)).countsByPostIds(any());
        verify(likeMapper, times(1)).findLikedPostIds(any(), any());
    }

    @Test
    void getNewPostsReturnsEmptyListForFollowingScope() {
        List<PostResponse> posts = postService.getNewPosts(5L, "following", 1L);

        assertThat(posts).isEmpty();
        verify(postMapper, never()).findNewerThan(any(), anyInt());
    }

    @Test
    void getNewPostsReturnsPostsNewerThanGivenId() {
        List<PostFeedRow> rows = List.of(feedRow(3L), feedRow(2L));
        when(postMapper.findNewerThan(1L, 50)).thenReturn(rows);

        List<PostResponse> posts = postService.getNewPosts(5L, "all", 1L);

        assertThat(posts).extracting(PostResponse::id).containsExactly(3L, 2L);
    }

    private PostFeedRow feedRow(Long id) {
        LocalDateTime now = LocalDateTime.now();
        return PostFeedRow.builder()
                .id(id)
                .userId(5L)
                .body("本文" + id)
                .createdAt(now)
                .updatedAt(now)
                .authorId(5L)
                .authorUsername("author")
                .authorDisplayName("投稿者")
                .build();
    }
}
