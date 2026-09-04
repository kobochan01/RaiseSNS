package com.raisesns.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdatePostRequest(
        @NotBlank
        @Size(min = 1, max = 280)
        String body
) {
}
