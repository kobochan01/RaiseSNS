package com.raisesns.backend.dto.response;

import java.time.LocalDateTime;

public record PostResponse(
        Long id,
        AuthorResponse author,
        String body,
        String imageUrl,
        int likeCount,
        int commentCount,
        boolean isLikedByMe,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
