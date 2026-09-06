package com.raisesns.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommentRequest(
        @NotBlank
        @Size(min = 1, max = 140)
        String body,
        Long parentCommentId
) {
}
