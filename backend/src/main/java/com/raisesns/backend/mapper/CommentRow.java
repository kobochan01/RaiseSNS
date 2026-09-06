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
public class CommentRow {

    private Long id;
    private Long postId;
    private Long userId;
    private Long parentCommentId;
    private String body;
    private LocalDateTime createdAt;
    private Long authorId;
    private String authorUsername;
    private String authorDisplayName;
    private String authorAvatarUrl;
}
