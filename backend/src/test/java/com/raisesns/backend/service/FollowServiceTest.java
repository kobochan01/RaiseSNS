package com.raisesns.backend.service;

import com.raisesns.backend.dto.response.FollowListResponse;
import com.raisesns.backend.dto.response.FollowResponse;
import com.raisesns.backend.entity.User;
import com.raisesns.backend.exception.SelfFollowException;
import com.raisesns.backend.exception.UserNotFoundException;
import com.raisesns.backend.mapper.FollowMapper;
import com.raisesns.backend.mapper.FollowUserRow;
import com.raisesns.backend.mapper.UserMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FollowServiceTest {

    private final FollowMapper followMapper = mock(FollowMapper.class);
    private final UserMapper userMapper = mock(UserMapper.class);
    private final FollowService followService = new FollowService(followMapper, userMapper);

    private User existingUser(Long id, String username) {
        return User.builder().id(id).username(username).displayName(username)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
    }

    @Test
    void followInsertsFollowAndReturnsCurrentState() {
        when(userMapper.findByUsername("jiro")).thenReturn(Optional.of(existingUser(10L, "jiro")));
        when(followMapper.countByFolloweeId(10L)).thenReturn(1);
        when(followMapper.existsByFollowerIdAndFolloweeId(5L, 10L)).thenReturn(true);

        FollowResponse response = followService.follow(5L, "jiro");

        verify(followMapper).insertIfAbsent(org.mockito.ArgumentMatchers.eq(5L), org.mockito.ArgumentMatchers.eq(10L), any());
        assertThat(response.followerCount()).isEqualTo(1);
        assertThat(response.isFollowedByMe()).isTrue();
    }

    @Test
    void followThrowsSelfFollowWhenFollowingSelf() {
        when(userMapper.findByUsername("taro")).thenReturn(Optional.of(existingUser(5L, "taro")));

        assertThatThrownBy(() -> followService.follow(5L, "taro")).isInstanceOf(SelfFollowException.class);
        verify(followMapper, never()).insertIfAbsent(anyLong(), anyLong(), any());
    }

    @Test
    void followThrowsUserNotFoundWhenTargetDoesNotExist() {
        when(userMapper.findByUsername("nobody")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> followService.follow(5L, "nobody")).isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void unfollowDeletesFollowAndReturnsCurrentState() {
        when(userMapper.findByUsername("jiro")).thenReturn(Optional.of(existingUser(10L, "jiro")));
        when(followMapper.countByFolloweeId(10L)).thenReturn(0);
        when(followMapper.existsByFollowerIdAndFolloweeId(5L, 10L)).thenReturn(false);

        FollowResponse response = followService.unfollow(5L, "jiro");

        verify(followMapper).deleteByFollowerIdAndFolloweeId(5L, 10L);
        assertThat(response.followerCount()).isZero();
        assertThat(response.isFollowedByMe()).isFalse();
    }

    @Test
    void unfollowThrowsUserNotFoundWhenTargetDoesNotExist() {
        when(userMapper.findByUsername("nobody")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> followService.unfollow(5L, "nobody")).isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void getFollowingReturnsMappedUserSummaries() {
        when(userMapper.findByUsername("taro")).thenReturn(Optional.of(existingUser(1L, "taro")));
        FollowUserRow row = FollowUserRow.builder().id(2L).username("jiro").displayName("次郎")
                .avatarUrl(null).isFollowedByMe(true).build();
        when(followMapper.findFollowing(1L, 99L, 50)).thenReturn(List.of(row));

        FollowListResponse response = followService.getFollowing(99L, "taro", null);

        assertThat(response.users()).hasSize(1);
        assertThat(response.users().get(0).username()).isEqualTo("jiro");
        assertThat(response.users().get(0).isFollowedByMe()).isTrue();
    }

    @Test
    void getFollowersReturnsMappedUserSummaries() {
        when(userMapper.findByUsername("taro")).thenReturn(Optional.of(existingUser(1L, "taro")));
        FollowUserRow row = FollowUserRow.builder().id(3L).username("saburo").displayName("三郎")
                .avatarUrl(null).isFollowedByMe(false).build();
        when(followMapper.findFollowers(1L, 99L, 50)).thenReturn(List.of(row));

        FollowListResponse response = followService.getFollowers(99L, "taro", null);

        assertThat(response.users()).hasSize(1);
        assertThat(response.users().get(0).username()).isEqualTo("saburo");
        assertThat(response.users().get(0).isFollowedByMe()).isFalse();
    }

    @Test
    void getFollowingClampsLimitToMaximum() {
        when(userMapper.findByUsername("taro")).thenReturn(Optional.of(existingUser(1L, "taro")));
        when(followMapper.findFollowing(1L, 99L, 50)).thenReturn(List.of());

        followService.getFollowing(99L, "taro", 1000);

        verify(followMapper).findFollowing(1L, 99L, 50);
    }

    @Test
    void getFollowingClampsLimitToMinimumWhenZeroOrNegative() {
        when(userMapper.findByUsername("taro")).thenReturn(Optional.of(existingUser(1L, "taro")));
        when(followMapper.findFollowing(1L, 99L, 1)).thenReturn(List.of());

        followService.getFollowing(99L, "taro", -1);

        verify(followMapper).findFollowing(1L, 99L, 1);
    }

    @Test
    void getFollowersClampsLimitToMaximum() {
        when(userMapper.findByUsername("taro")).thenReturn(Optional.of(existingUser(1L, "taro")));
        when(followMapper.findFollowers(1L, 99L, 50)).thenReturn(List.of());

        followService.getFollowers(99L, "taro", 1000);

        verify(followMapper).findFollowers(1L, 99L, 50);
    }

    @Test
    void getFollowersClampsLimitToMinimumWhenZeroOrNegative() {
        when(userMapper.findByUsername("taro")).thenReturn(Optional.of(existingUser(1L, "taro")));
        when(followMapper.findFollowers(1L, 99L, 1)).thenReturn(List.of());

        followService.getFollowers(99L, "taro", 0);

        verify(followMapper).findFollowers(1L, 99L, 1);
    }
}
