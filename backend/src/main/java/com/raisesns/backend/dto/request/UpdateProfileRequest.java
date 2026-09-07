package com.raisesns.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank
        @Size(min = 1, max = 50)
        String displayName,
        @Size(max = 160)
        String bio,
        @Size(max = 500)
        String avatarUrl
) {
    public UpdateProfileRequest(String displayName, String bio) {
        this(displayName, bio, null);
    }
}
