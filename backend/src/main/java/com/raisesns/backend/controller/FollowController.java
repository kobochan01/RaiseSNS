package com.raisesns.backend.controller;

import com.raisesns.backend.dto.response.FollowListResponse;
import com.raisesns.backend.dto.response.FollowResponse;
import com.raisesns.backend.service.FollowService;
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
public class FollowController {

    private final FollowService followService;

    public FollowController(FollowService followService) {
        this.followService = followService;
    }

    @PostMapping("/follow")
    public ResponseEntity<FollowResponse> follow(@AuthenticationPrincipal Long userId, @PathVariable String username) {
        return ResponseEntity.ok(followService.follow(userId, username));
    }

    @DeleteMapping("/follow")
    public ResponseEntity<FollowResponse> unfollow(@AuthenticationPrincipal Long userId, @PathVariable String username) {
        return ResponseEntity.ok(followService.unfollow(userId, username));
    }

    @GetMapping("/following")
    public ResponseEntity<FollowListResponse> getFollowing(@AuthenticationPrincipal Long userId,
                                                            @PathVariable String username,
                                                            @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(followService.getFollowing(userId, username, limit));
    }

    @GetMapping("/followers")
    public ResponseEntity<FollowListResponse> getFollowers(@AuthenticationPrincipal Long userId,
                                                            @PathVariable String username,
                                                            @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(followService.getFollowers(userId, username, limit));
    }
}
