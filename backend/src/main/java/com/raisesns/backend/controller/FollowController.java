package com.raisesns.backend.controller;

import com.raisesns.backend.dto.response.FollowListResponse;
import com.raisesns.backend.dto.response.FollowResponse;
import com.raisesns.backend.service.FollowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/{username}")
@Tag(name = "フォロー")
public class FollowController {

    private final FollowService followService;

    public FollowController(FollowService followService) {
        this.followService = followService;
    }

    @Operation(summary = "フォロー")
    @PostMapping("/follow")
    public ResponseEntity<FollowResponse> follow(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId, @PathVariable String username) {
        return ResponseEntity.ok(followService.follow(userId, username));
    }

    @Operation(summary = "フォロー解除")
    @DeleteMapping("/follow")
    public ResponseEntity<FollowResponse> unfollow(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId, @PathVariable String username) {
        return ResponseEntity.ok(followService.unfollow(userId, username));
    }

    @Operation(summary = "フォロー中一覧取得")
    @GetMapping("/following")
    public ResponseEntity<FollowListResponse> getFollowing(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @PathVariable String username,
            @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(followService.getFollowing(userId, username, limit));
    }

    @Operation(summary = "フォロワー一覧取得")
    @GetMapping("/followers")
    public ResponseEntity<FollowListResponse> getFollowers(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @PathVariable String username,
            @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(followService.getFollowers(userId, username, limit));
    }
}
