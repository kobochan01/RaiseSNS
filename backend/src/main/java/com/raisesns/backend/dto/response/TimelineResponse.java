package com.raisesns.backend.dto.response;

import java.util.List;

public record TimelineResponse(List<PostResponse> posts, Long nextCursor) {
}
