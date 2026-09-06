package com.raisesns.backend.controller;

import com.raisesns.backend.dto.response.CommentCountResponse;
import com.raisesns.backend.service.CommentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<CommentCountResponse> delete(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        int commentCount = commentService.delete(userId, id);
        return ResponseEntity.ok(new CommentCountResponse(commentCount));
    }
}
