package com.raisesns.backend.dto.response;

public record ProfileResponse(Long id, String username, String displayName, String bio, String avatarUrl,
                               int followerCount, int followingCount, boolean isFollowedByMe) {
}
