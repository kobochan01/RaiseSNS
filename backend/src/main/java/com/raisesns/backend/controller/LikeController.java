package com.raisesns.backend.controller;

import com.raisesns.backend.dto.response.LikeResponse;
import com.raisesns.backend.service.LikeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts/{postId}/likes")
@Tag(name = "いいね")
public class LikeController {

    private final LikeService likeService;

    public LikeController(LikeService likeService) {
        this.likeService = likeService;
    }

    @Operation(summary = "いいね")
    @PostMapping
    public ResponseEntity<LikeResponse> like(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId, @PathVariable Long postId) {
        return ResponseEntity.ok(likeService.like(userId, postId));
    }

    @Operation(summary = "いいね解除")
    @DeleteMapping
    public ResponseEntity<LikeResponse> unlike(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId, @PathVariable Long postId) {
        return ResponseEntity.ok(likeService.unlike(userId, postId));
    }
}
