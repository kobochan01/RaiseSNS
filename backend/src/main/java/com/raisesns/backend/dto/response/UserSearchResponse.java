package com.raisesns.backend.dto.response;

import java.util.List;

public record UserSearchResponse(List<UserSearchResultResponse> results, int totalCount) {
}
