package com.raisesns.backend.mapper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FollowUserRow {

    private Long id;
    private String username;
    private String displayName;
    private String avatarUrl;
    private boolean isFollowedByMe;
}
