package com.raisesns.backend.controller;

import com.raisesns.backend.dto.request.UpdateProfileRequest;
import com.raisesns.backend.dto.response.ProfileResponse;
import com.raisesns.backend.dto.response.TimelineResponse;
import com.raisesns.backend.dto.response.UserSearchResponse;
import com.raisesns.backend.service.PostService;
import com.raisesns.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@Tag(name = "ユーザー")
public class UserController {

    private final UserService userService;
    private final PostService postService;

    public UserController(UserService userService, PostService postService) {
        this.userService = userService;
        this.postService = postService;
    }

    @Operation(summary = "ユーザー検索")
    @GetMapping("/search")
    public ResponseEntity<UserSearchResponse> searchUsers(@RequestParam String keyword,
                                                            @RequestParam(required = false) Integer limit,
                                                            @RequestParam(required = false) Integer offset) {
        return ResponseEntity.ok(userService.searchUsers(keyword, limit, offset));
    }

    @Operation(summary = "プロフィール取得")
    @GetMapping("/{username}")
    public ResponseEntity<ProfileResponse> getProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @PathVariable String username) {
        return ResponseEntity.ok(userService.getProfile(userId, username));
    }

    @Operation(summary = "プロフィール編集")
    @PutMapping("/{username}")
    public ResponseEntity<ProfileResponse> updateProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @PathVariable String username,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(userId, username, request));
    }

    @Operation(summary = "ユーザーの投稿一覧取得")
    @GetMapping("/{username}/posts")
    public ResponseEntity<TimelineResponse> getUserPosts(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @PathVariable String username,
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(postService.getUserPosts(userId, username, cursor, limit));
    }
}
