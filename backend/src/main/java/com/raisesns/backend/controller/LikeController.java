package com.raisesns.backend.controller;

import com.raisesns.backend.dto.response.LikeResponse;
import com.raisesns.backend.service.LikeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts/{postId}/likes")
public class LikeController {

    private final LikeService likeService;

    public LikeController(LikeService likeService) {
        this.likeService = likeService;
    }

    @PostMapping
    public ResponseEntity<LikeResponse> like(@AuthenticationPrincipal Long userId, @PathVariable Long postId) {
        return ResponseEntity.ok(likeService.like(userId, postId));
    }

    @DeleteMapping
    public ResponseEntity<LikeResponse> unlike(@AuthenticationPrincipal Long userId, @PathVariable Long postId) {
        return ResponseEntity.ok(likeService.unlike(userId, postId));
    }
}
