package com.raisesns.backend.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Like {

    private Long id;
    private Long postId;
    private Long userId;
    private LocalDateTime createdAt;
}
