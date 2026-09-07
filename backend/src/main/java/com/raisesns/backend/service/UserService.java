package com.raisesns.backend.service;

import com.raisesns.backend.dto.request.UpdateProfileRequest;
import com.raisesns.backend.dto.response.ProfileResponse;
import com.raisesns.backend.dto.response.UserSearchResponse;
import com.raisesns.backend.dto.response.UserSearchResultResponse;
import com.raisesns.backend.entity.User;
import com.raisesns.backend.exception.BlankSearchKeywordException;
import com.raisesns.backend.exception.ProfileAccessDeniedException;
import com.raisesns.backend.exception.UserNotFoundException;
import com.raisesns.backend.mapper.FollowMapper;
import com.raisesns.backend.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private static final int DEFAULT_SEARCH_LIMIT = 20;
    private static final int MAX_SEARCH_LIMIT = 50;

    private final UserMapper userMapper;
    private final FollowMapper followMapper;

    public UserService(UserMapper userMapper, FollowMapper followMapper) {
        this.userMapper = userMapper;
        this.followMapper = followMapper;
    }

    public ProfileResponse getProfile(Long viewerId, String username) {
        User target = findByUsername(username);
        return toProfileResponse(viewerId, target);
    }

    @Transactional
    public ProfileResponse updateProfile(Long userId, String username, UpdateProfileRequest request) {
        User target = findByUsername(username);
        if (!target.getId().equals(userId)) {
            throw new ProfileAccessDeniedException();
        }

        userMapper.updateProfile(target.getId(), request.displayName(), request.bio(), LocalDateTime.now());
        User updated = findByUsername(username);
        return toProfileResponse(userId, updated);
    }

    public UserSearchResponse searchUsers(String keyword, Integer limit, Integer offset) {
        if (keyword == null || keyword.isBlank()) {
            throw new BlankSearchKeywordException();
        }

        int normalizedLimit = normalizeLimit(limit);
        int normalizedOffset = offset == null ? 0 : Math.max(0, offset);

        List<User> users = userMapper.searchByUsername(keyword, normalizedLimit, normalizedOffset);
        int totalCount = userMapper.countByUsernameContaining(keyword);

        List<UserSearchResultResponse> results = users.stream()
                .map(user -> new UserSearchResultResponse(user.getUsername(), user.getDisplayName(), user.getAvatarUrl()))
                .collect(Collectors.toList());
        return new UserSearchResponse(results, totalCount);
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_SEARCH_LIMIT;
        }
        return Math.max(1, Math.min(limit, MAX_SEARCH_LIMIT));
    }

    private User findByUsername(String username) {
        return userMapper.findByUsername(username).orElseThrow(() -> new UserNotFoundException(username));
    }

    private ProfileResponse toProfileResponse(Long viewerId, User target) {
        int followerCount = followMapper.countByFolloweeId(target.getId());
        int followingCount = followMapper.countByFollowerId(target.getId());
        boolean isFollowedByMe = followMapper.existsByFollowerIdAndFolloweeId(viewerId, target.getId());
        return new ProfileResponse(target.getId(), target.getUsername(), target.getDisplayName(), target.getBio(),
                target.getAvatarUrl(), followerCount, followingCount, isFollowedByMe);
    }
}
