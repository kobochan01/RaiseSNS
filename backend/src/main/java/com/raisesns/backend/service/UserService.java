package com.raisesns.backend.service;

import com.raisesns.backend.dto.request.UpdateProfileRequest;
import com.raisesns.backend.dto.response.ProfileResponse;
import com.raisesns.backend.entity.User;
import com.raisesns.backend.exception.ProfileAccessDeniedException;
import com.raisesns.backend.exception.UserNotFoundException;
import com.raisesns.backend.mapper.FollowMapper;
import com.raisesns.backend.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class UserService {

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
