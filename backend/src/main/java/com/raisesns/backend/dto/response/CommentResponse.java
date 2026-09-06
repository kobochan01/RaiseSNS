package com.raisesns.backend.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record CommentResponse(
        Long id,
        AuthorResponse author,
        String body,
        Long parentCommentId,
        LocalDateTime createdAt,
        List<CommentResponse> replies
) {
}
