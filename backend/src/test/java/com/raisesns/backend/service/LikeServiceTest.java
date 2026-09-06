package com.raisesns.backend.service;

import com.raisesns.backend.dto.response.LikeResponse;
import com.raisesns.backend.entity.Post;
import com.raisesns.backend.exception.PostNotFoundException;
import com.raisesns.backend.mapper.LikeMapper;
import com.raisesns.backend.mapper.PostMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LikeServiceTest {

    private final LikeMapper likeMapper = mock(LikeMapper.class);
    private final PostMapper postMapper = mock(PostMapper.class);
    private final LikeService likeService = new LikeService(likeMapper, postMapper);

    private Post existingPost(Long id) {
        LocalDateTime now = LocalDateTime.now();
        return Post.builder().id(id).userId(1L).body("投稿").createdAt(now).updatedAt(now).build();
    }

    @Test
    void likeInsertsLikeAndReturnsCurrentState() {
        when(postMapper.findById(10L)).thenReturn(Optional.of(existingPost(10L)));
        when(likeMapper.countByPostId(10L)).thenReturn(1);
        when(likeMapper.existsByPostIdAndUserId(10L, 5L)).thenReturn(true);

        LikeResponse response = likeService.like(5L, 10L);

        verify(likeMapper).insertIfAbsent(eq(10L), eq(5L), any());
        assertThat(response.likeCount()).isEqualTo(1);
        assertThat(response.isLikedByMe()).isTrue();
    }

    @Test
    void likeThrowsPostNotFoundWhenPostDoesNotExist() {
        when(postMapper.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> likeService.like(5L, 10L)).isInstanceOf(PostNotFoundException.class);
        verify(likeMapper, never()).insertIfAbsent(anyLong(), anyLong(), any());
    }

    @Test
    void unlikeDeletesLikeAndReturnsCurrentState() {
        when(postMapper.findById(10L)).thenReturn(Optional.of(existingPost(10L)));
        when(likeMapper.countByPostId(10L)).thenReturn(0);
        when(likeMapper.existsByPostIdAndUserId(10L, 5L)).thenReturn(false);

        LikeResponse response = likeService.unlike(5L, 10L);

        verify(likeMapper).deleteByPostIdAndUserId(10L, 5L);
        assertThat(response.likeCount()).isZero();
        assertThat(response.isLikedByMe()).isFalse();
    }

    @Test
    void unlikeThrowsPostNotFoundWhenPostDoesNotExist() {
        when(postMapper.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> likeService.unlike(5L, 10L)).isInstanceOf(PostNotFoundException.class);
        verify(likeMapper, never()).deleteByPostIdAndUserId(anyLong(), anyLong());
    }
}
