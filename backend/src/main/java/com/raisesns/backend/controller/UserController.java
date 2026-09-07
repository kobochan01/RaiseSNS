package com.raisesns.backend.controller;

import com.raisesns.backend.dto.request.UpdateProfileRequest;
import com.raisesns.backend.dto.response.ProfileResponse;
import com.raisesns.backend.dto.response.TimelineResponse;
import com.raisesns.backend.dto.response.UserSearchResponse;
import com.raisesns.backend.service.PostService;
import com.raisesns.backend.service.UserService;
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
public class UserController {

    private final UserService userService;
    private final PostService postService;

    public UserController(UserService userService, PostService postService) {
        this.userService = userService;
        this.postService = postService;
    }

    @GetMapping("/search")
    public ResponseEntity<UserSearchResponse> searchUsers(@RequestParam String keyword,
                                                            @RequestParam(required = false) Integer limit,
                                                            @RequestParam(required = false) Integer offset) {
        return ResponseEntity.ok(userService.searchUsers(keyword, limit, offset));
    }

    @GetMapping("/{username}")
    public ResponseEntity<ProfileResponse> getProfile(@AuthenticationPrincipal Long userId,
                                                        @PathVariable String username) {
        return ResponseEntity.ok(userService.getProfile(userId, username));
    }

    @PutMapping("/{username}")
    public ResponseEntity<ProfileResponse> updateProfile(@AuthenticationPrincipal Long userId,
                                                           @PathVariable String username,
                                                           @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(userId, username, request));
    }

    @GetMapping("/{username}/posts")
    public ResponseEntity<TimelineResponse> getUserPosts(@AuthenticationPrincipal Long userId,
                                                          @PathVariable String username,
                                                          @RequestParam(required = false) Long cursor,
                                                          @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(postService.getUserPosts(userId, username, cursor, limit));
    }
}
