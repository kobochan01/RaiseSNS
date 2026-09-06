package com.raisesns.backend.service;

import com.raisesns.backend.dto.response.FollowListResponse;
import com.raisesns.backend.dto.response.FollowResponse;
import com.raisesns.backend.dto.response.UserSummaryResponse;
import com.raisesns.backend.entity.User;
import com.raisesns.backend.exception.SelfFollowException;
import com.raisesns.backend.exception.UserNotFoundException;
import com.raisesns.backend.mapper.FollowMapper;
import com.raisesns.backend.mapper.FollowUserRow;
import com.raisesns.backend.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FollowService {

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 50;

    private final FollowMapper followMapper;
    private final UserMapper userMapper;

    public FollowService(FollowMapper followMapper, UserMapper userMapper) {
        this.followMapper = followMapper;
        this.userMapper = userMapper;
    }

    @Transactional
    public FollowResponse follow(Long followerId, String targetUsername) {
        User target = findByUsername(targetUsername);
        if (target.getId().equals(followerId)) {
            throw new SelfFollowException();
        }

        followMapper.insertIfAbsent(followerId, target.getId(), LocalDateTime.now());
        return currentState(target.getId(), followerId);
    }

    @Transactional
    public FollowResponse unfollow(Long followerId, String targetUsername) {
        User target = findByUsername(targetUsername);
        followMapper.deleteByFollowerIdAndFolloweeId(followerId, target.getId());
        return currentState(target.getId(), followerId);
    }

    public FollowListResponse getFollowing(Long viewerId, String targetUsername, Integer limit) {
        User target = findByUsername(targetUsername);
        List<FollowUserRow> rows = followMapper.findFollowing(target.getId(), viewerId, normalizeLimit(limit));
        return toFollowListResponse(rows);
    }

    public FollowListResponse getFollowers(Long viewerId, String targetUsername, Integer limit) {
        User target = findByUsername(targetUsername);
        List<FollowUserRow> rows = followMapper.findFollowers(target.getId(), viewerId, normalizeLimit(limit));
        return toFollowListResponse(rows);
    }

    private User findByUsername(String username) {
        return userMapper.findByUsername(username).orElseThrow(() -> new UserNotFoundException(username));
    }

    private FollowResponse currentState(Long targetId, Long viewerId) {
        return new FollowResponse(followMapper.countByFolloweeId(targetId),
                followMapper.existsByFollowerIdAndFolloweeId(viewerId, targetId));
    }

    private FollowListResponse toFollowListResponse(List<FollowUserRow> rows) {
        List<UserSummaryResponse> users = rows.stream()
                .map(row -> new UserSummaryResponse(row.getId(), row.getUsername(), row.getDisplayName(),
                        row.getAvatarUrl(), row.isFollowedByMe()))
                .collect(Collectors.toList());
        return new FollowListResponse(users);
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        return Math.max(1, Math.min(limit, MAX_LIMIT));
    }
}
