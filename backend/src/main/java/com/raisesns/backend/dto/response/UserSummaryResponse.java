package com.raisesns.backend.dto.response;

public record UserSummaryResponse(Long id, String username, String displayName, String avatarUrl,
                                   boolean isFollowedByMe) {
}
