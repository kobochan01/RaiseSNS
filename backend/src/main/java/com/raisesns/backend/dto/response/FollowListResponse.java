package com.raisesns.backend.dto.response;

import java.util.List;

public record FollowListResponse(List<UserSummaryResponse> users) {
}
