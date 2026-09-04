package com.raisesns.backend.mapper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostFeedRow {

    private Long id;
    private Long userId;
    private String body;
    private String imageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long authorId;
    private String authorUsername;
    private String authorDisplayName;
    private String authorAvatarUrl;
}
