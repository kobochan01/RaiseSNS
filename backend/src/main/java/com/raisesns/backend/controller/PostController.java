package com.raisesns.backend.controller;

import com.raisesns.backend.dto.request.CreatePostRequest;
import com.raisesns.backend.dto.request.UpdatePostRequest;
import com.raisesns.backend.dto.response.PostResponse;
import com.raisesns.backend.dto.response.TimelineResponse;
import com.raisesns.backend.service.PostService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping
    public ResponseEntity<PostResponse> create(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CreatePostRequest request) {
        PostResponse response = postService.create(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<TimelineResponse> getTimeline(
            @RequestParam(defaultValue = "all") String scope,
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(postService.getTimeline(scope, cursor, limit));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PostResponse> update(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id,
            @Valid @RequestBody UpdatePostRequest request) {
        return ResponseEntity.ok(postService.update(userId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        postService.delete(userId, id);
        return ResponseEntity.noContent().build();
    }
}
