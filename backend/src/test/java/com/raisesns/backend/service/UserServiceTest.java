package com.raisesns.backend.service;

import com.raisesns.backend.dto.request.UpdateProfileRequest;
import com.raisesns.backend.dto.response.ProfileResponse;
import com.raisesns.backend.dto.response.UserSearchResponse;
import com.raisesns.backend.entity.User;
import com.raisesns.backend.exception.BlankSearchKeywordException;
import com.raisesns.backend.exception.ProfileAccessDeniedException;
import com.raisesns.backend.exception.UserNotFoundException;
import com.raisesns.backend.mapper.FollowMapper;
import com.raisesns.backend.mapper.UserMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTest {

    private final UserMapper userMapper = mock(UserMapper.class);
    private final FollowMapper followMapper = mock(FollowMapper.class);
    private final UserService userService = new UserService(userMapper, followMapper);

    private User existingUser(Long id, String username) {
        return User.builder().id(id).username(username).displayName("表示名").bio("自己紹介")
                .avatarUrl(null).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
    }

    @Test
    void getProfileReturnsProfileWithCountsAndFollowState() {
        when(userMapper.findByUsername("taro")).thenReturn(Optional.of(existingUser(1L, "taro")));
        when(followMapper.countByFolloweeId(1L)).thenReturn(3);
        when(followMapper.countByFollowerId(1L)).thenReturn(5);
        when(followMapper.existsByFollowerIdAndFolloweeId(2L, 1L)).thenReturn(true);

        ProfileResponse response = userService.getProfile(2L, "taro");

        assertThat(response.username()).isEqualTo("taro");
        assertThat(response.followerCount()).isEqualTo(3);
        assertThat(response.followingCount()).isEqualTo(5);
        assertThat(response.isFollowedByMe()).isTrue();
    }

    @Test
    void getProfileThrowsUserNotFoundWhenUserDoesNotExist() {
        when(userMapper.findByUsername("nobody")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getProfile(2L, "nobody")).isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void updateProfileSucceedsWhenRequesterIsOwner() {
        when(userMapper.findByUsername("taro")).thenReturn(Optional.of(existingUser(1L, "taro")));
        when(followMapper.countByFolloweeId(1L)).thenReturn(0);
        when(followMapper.countByFollowerId(1L)).thenReturn(0);
        when(followMapper.existsByFollowerIdAndFolloweeId(1L, 1L)).thenReturn(false);

        ProfileResponse response = userService.updateProfile(1L, "taro", new UpdateProfileRequest("新しい名前", "新しい自己紹介"));

        verify(userMapper).updateProfile(eq(1L), any(), any(), any(), any());
        assertThat(response.username()).isEqualTo("taro");
    }

    @Test
    void updateProfileThrowsAccessDeniedWhenRequesterIsNotOwner() {
        when(userMapper.findByUsername("taro")).thenReturn(Optional.of(existingUser(1L, "taro")));

        assertThatThrownBy(() -> userService.updateProfile(999L, "taro", new UpdateProfileRequest("名前", null)))
                .isInstanceOf(ProfileAccessDeniedException.class);
        verify(userMapper, never()).updateProfile(anyLong(), anyString(), any(), any(), any());
    }

    @Test
    void updateProfilePassesAvatarUrlToMapper() {
        when(userMapper.findByUsername("taro")).thenReturn(Optional.of(existingUser(1L, "taro")));
        when(followMapper.countByFolloweeId(1L)).thenReturn(0);
        when(followMapper.countByFollowerId(1L)).thenReturn(0);
        when(followMapper.existsByFollowerIdAndFolloweeId(1L, 1L)).thenReturn(false);

        userService.updateProfile(1L, "taro",
                new UpdateProfileRequest("新しい名前", "新しい自己紹介", "https://example-bucket.s3.ap-northeast-1.amazonaws.com/avatars/a.png"));

        verify(userMapper).updateProfile(eq(1L), any(), any(),
                eq("https://example-bucket.s3.ap-northeast-1.amazonaws.com/avatars/a.png"), any());
    }

    @Test
    void updateProfileThrowsUserNotFoundWhenUserDoesNotExist() {
        when(userMapper.findByUsername("nobody")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateProfile(1L, "nobody", new UpdateProfileRequest("名前", null)))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void searchUsersReturnsResultsAndTotalCount() {
        when(userMapper.searchByUsername("tar", 20, 0)).thenReturn(List.of(existingUser(1L, "taro")));
        when(userMapper.countByUsernameContaining("tar")).thenReturn(1);

        UserSearchResponse response = userService.searchUsers("tar", null, null);

        assertThat(response.results()).hasSize(1);
        assertThat(response.results().get(0).username()).isEqualTo("taro");
        assertThat(response.totalCount()).isEqualTo(1);
    }

    @Test
    void searchUsersClampsLimitToMaximum() {
        when(userMapper.searchByUsername("tar", 50, 0)).thenReturn(List.of());
        when(userMapper.countByUsernameContaining("tar")).thenReturn(0);

        userService.searchUsers("tar", 1000, null);

        verify(userMapper).searchByUsername("tar", 50, 0);
    }

    @Test
    void searchUsersThrowsWhenKeywordIsBlank() {
        assertThatThrownBy(() -> userService.searchUsers("  ", null, null))
                .isInstanceOf(BlankSearchKeywordException.class);
        verify(userMapper, never()).searchByUsername(anyString(), anyInt(), anyInt());
    }

    @Test
    void searchUsersThrowsWhenKeywordIsNull() {
        assertThatThrownBy(() -> userService.searchUsers(null, null, null))
                .isInstanceOf(BlankSearchKeywordException.class);
    }
}
