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
public class Follow {

    private Long followerId;
    private Long followeeId;
    private LocalDateTime createdAt;
}
