package com.raisesns.backend.service;

import com.raisesns.backend.dto.request.CreateCommentRequest;
import com.raisesns.backend.dto.response.AuthorResponse;
import com.raisesns.backend.dto.response.CommentResponse;
import com.raisesns.backend.entity.Comment;
import com.raisesns.backend.entity.User;
import com.raisesns.backend.exception.CommentAccessDeniedException;
import com.raisesns.backend.exception.CommentNotFoundException;
import com.raisesns.backend.exception.InvalidCommentParentException;
import com.raisesns.backend.exception.PostNotFoundException;
import com.raisesns.backend.mapper.CommentMapper;
import com.raisesns.backend.mapper.CommentRow;
import com.raisesns.backend.mapper.PostMapper;
import com.raisesns.backend.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CommentService {

    private final CommentMapper commentMapper;
    private final PostMapper postMapper;
    private final UserMapper userMapper;

    public CommentService(CommentMapper commentMapper, PostMapper postMapper, UserMapper userMapper) {
        this.commentMapper = commentMapper;
        this.postMapper = postMapper;
        this.userMapper = userMapper;
    }

    @Transactional
    public CommentResponse create(Long userId, Long postId, CreateCommentRequest request) {
        postMapper.findById(postId).orElseThrow(() -> new PostNotFoundException(postId));

        Long parentCommentId = request.parentCommentId();
        if (parentCommentId != null) {
            Comment parent = commentMapper.findById(parentCommentId)
                    .orElseThrow(() -> new CommentNotFoundException(parentCommentId));
            if (!parent.getPostId().equals(postId)) {
                throw new InvalidCommentParentException(parentCommentId);
            }
        }

        LocalDateTime now = LocalDateTime.now();
        Comment comment = Comment.builder()
                .postId(postId)
                .userId(userId)
                .parentCommentId(parentCommentId)
                .body(request.body())
                .createdAt(now)
                .build();
        commentMapper.insert(comment);

        User author = userMapper.findById(userId).orElseThrow(IllegalStateException::new);
        AuthorResponse authorResponse =
                new AuthorResponse(author.getId(), author.getUsername(), author.getDisplayName(), author.getAvatarUrl());
        return new CommentResponse(comment.getId(), authorResponse, comment.getBody(),
                comment.getParentCommentId(), comment.getCreatedAt(), List.of());
    }

    public List<CommentResponse> getComments(Long postId) {
        postMapper.findById(postId).orElseThrow(() -> new PostNotFoundException(postId));

        // Collectors.groupingBy はnullキーを許容しないため、トップレベルコメント(親なし)は 0L で代表させる
        List<CommentRow> rows = commentMapper.findByPostId(postId);
        Map<Long, List<CommentRow>> byParent = rows.stream()
                .collect(Collectors.groupingBy(row -> row.getParentCommentId() == null ? 0L : row.getParentCommentId()));
        return buildReplies(0L, byParent);
    }

    @Transactional
    public int delete(Long userId, Long commentId) {
        Comment comment = commentMapper.findById(commentId).orElseThrow(() -> new CommentNotFoundException(commentId));
        if (!comment.getUserId().equals(userId)) {
            throw new CommentAccessDeniedException();
        }

        Long postId = comment.getPostId();
        commentMapper.deleteById(commentId);
        return commentMapper.countByPostId(postId);
    }

    private List<CommentResponse> buildReplies(Long parentId, Map<Long, List<CommentRow>> byParent) {
        return byParent.getOrDefault(parentId, List.of()).stream()
                .map(row -> toCommentResponse(row, byParent))
                .collect(Collectors.toList());
    }

    private CommentResponse toCommentResponse(CommentRow row, Map<Long, List<CommentRow>> byParent) {
        AuthorResponse author = new AuthorResponse(row.getAuthorId(), row.getAuthorUsername(),
                row.getAuthorDisplayName(), row.getAuthorAvatarUrl());
        List<CommentResponse> replies = buildReplies(row.getId(), byParent);
        return new CommentResponse(row.getId(), author, row.getBody(), row.getParentCommentId(),
                row.getCreatedAt(), replies);
    }
}
