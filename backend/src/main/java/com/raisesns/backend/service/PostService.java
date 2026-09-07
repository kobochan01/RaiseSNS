package com.raisesns.backend.service;

import com.raisesns.backend.dto.request.CreatePostRequest;
import com.raisesns.backend.dto.request.UpdatePostRequest;
import com.raisesns.backend.dto.response.AuthorResponse;
import com.raisesns.backend.dto.response.PostResponse;
import com.raisesns.backend.dto.response.TimelineResponse;
import com.raisesns.backend.entity.Post;
import com.raisesns.backend.entity.User;
import com.raisesns.backend.exception.PostAccessDeniedException;
import com.raisesns.backend.exception.PostNotFoundException;
import com.raisesns.backend.exception.UserNotFoundException;
import com.raisesns.backend.mapper.CommentMapper;
import com.raisesns.backend.mapper.LikeMapper;
import com.raisesns.backend.mapper.PostCountRow;
import com.raisesns.backend.mapper.PostFeedRow;
import com.raisesns.backend.mapper.PostMapper;
import com.raisesns.backend.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PostService {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 50;
    private static final String SCOPE_FOLLOWING = "following";

    private final PostMapper postMapper;
    private final UserMapper userMapper;
    private final LikeMapper likeMapper;
    private final CommentMapper commentMapper;

    public PostService(PostMapper postMapper, UserMapper userMapper, LikeMapper likeMapper, CommentMapper commentMapper) {
        this.postMapper = postMapper;
        this.userMapper = userMapper;
        this.likeMapper = likeMapper;
        this.commentMapper = commentMapper;
    }

    @Transactional
    public PostResponse create(Long userId, CreatePostRequest request) {
        LocalDateTime now = LocalDateTime.now();
        Post post = Post.builder()
                .userId(userId)
                .body(request.body())
                .imageUrl(request.imageUrl())
                .createdAt(now)
                .updatedAt(now)
                .build();
        postMapper.insert(post);

        User author = userMapper.findById(userId).orElseThrow(IllegalStateException::new);
        return toPostResponse(post, author, 0, 0, false);
    }

    @Transactional
    public PostResponse update(Long userId, Long postId, UpdatePostRequest request) {
        Post post = postMapper.findById(postId).orElseThrow(() -> new PostNotFoundException(postId));
        assertOwner(post, userId);

        Post updated = Post.builder()
                .id(post.getId())
                .userId(post.getUserId())
                .body(request.body())
                .imageUrl(post.getImageUrl())
                .createdAt(post.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();
        postMapper.update(updated);

        User author = userMapper.findById(userId).orElseThrow(IllegalStateException::new);
        int likeCount = likeMapper.countByPostId(postId);
        int commentCount = commentMapper.countByPostId(postId);
        boolean isLikedByMe = likeMapper.existsByPostIdAndUserId(postId, userId);
        return toPostResponse(updated, author, likeCount, commentCount, isLikedByMe);
    }

    @Transactional
    public void delete(Long userId, Long postId) {
        Post post = postMapper.findById(postId).orElseThrow(() -> new PostNotFoundException(postId));
        assertOwner(post, userId);

        postMapper.deleteById(postId);
    }

    public TimelineResponse getTimeline(Long currentUserId, String scope, Long cursor, Integer limit) {
        if (SCOPE_FOLLOWING.equals(scope)) {
            return new TimelineResponse(List.of(), null);
        }

        int normalizedLimit = normalizeLimit(limit);
        List<PostFeedRow> rows = postMapper.findFeed(cursor, normalizedLimit + 1);

        boolean hasMore = rows.size() > normalizedLimit;
        List<PostFeedRow> pageRows = hasMore ? rows.subList(0, normalizedLimit) : rows;
        Long nextCursor = hasMore ? pageRows.get(pageRows.size() - 1).getId() : null;

        List<PostResponse> posts = toPostResponses(currentUserId, pageRows);
        return new TimelineResponse(posts, nextCursor);
    }

    public List<PostResponse> getNewPosts(Long currentUserId, String scope, Long sinceId) {
        if (SCOPE_FOLLOWING.equals(scope)) {
            return List.of();
        }

        List<PostFeedRow> rows = postMapper.findNewerThan(sinceId, MAX_LIMIT);
        return toPostResponses(currentUserId, rows);
    }

    public TimelineResponse getUserPosts(Long currentUserId, String username, Long cursor, Integer limit) {
        User target = userMapper.findByUsername(username).orElseThrow(() -> new UserNotFoundException(username));

        int normalizedLimit = normalizeLimit(limit);
        List<PostFeedRow> rows = postMapper.findByUserId(target.getId(), cursor, normalizedLimit + 1);

        boolean hasMore = rows.size() > normalizedLimit;
        List<PostFeedRow> pageRows = hasMore ? rows.subList(0, normalizedLimit) : rows;
        Long nextCursor = hasMore ? pageRows.get(pageRows.size() - 1).getId() : null;

        List<PostResponse> posts = toPostResponses(currentUserId, pageRows);
        return new TimelineResponse(posts, nextCursor);
    }

    // 投稿1件ごとにいいね/コメント数を問い合わせるとN+1になるため、対象投稿IDをまとめてバッチ集計する
    private List<PostResponse> toPostResponses(Long currentUserId, List<PostFeedRow> rows) {
        List<Long> postIds = rows.stream().map(PostFeedRow::getId).collect(Collectors.toList());
        if (postIds.isEmpty()) {
            return List.of();
        }

        Map<Long, Integer> likeCounts = toCountMap(likeMapper.countsByPostIds(postIds));
        Map<Long, Integer> commentCounts = toCountMap(commentMapper.countsByPostIds(postIds));
        Set<Long> likedByMe = new HashSet<>(likeMapper.findLikedPostIds(currentUserId, postIds));

        return rows.stream()
                .map(row -> toPostResponse(row,
                        likeCounts.getOrDefault(row.getId(), 0),
                        commentCounts.getOrDefault(row.getId(), 0),
                        likedByMe.contains(row.getId())))
                .collect(Collectors.toList());
    }

    private Map<Long, Integer> toCountMap(List<PostCountRow> rows) {
        return rows.stream().collect(Collectors.toMap(PostCountRow::getPostId, PostCountRow::getCount));
    }

    private void assertOwner(Post post, Long userId) {
        if (!post.getUserId().equals(userId)) {
            throw new PostAccessDeniedException();
        }
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        return Math.max(1, Math.min(limit, MAX_LIMIT));
    }

    private PostResponse toPostResponse(Post post, User author, int likeCount, int commentCount, boolean isLikedByMe) {
        AuthorResponse authorResponse =
                new AuthorResponse(author.getId(), author.getUsername(), author.getDisplayName(), author.getAvatarUrl());
        return new PostResponse(post.getId(), authorResponse, post.getBody(), post.getImageUrl(),
                likeCount, commentCount, isLikedByMe, post.getCreatedAt(), post.getUpdatedAt());
    }

    private PostResponse toPostResponse(PostFeedRow row, int likeCount, int commentCount, boolean isLikedByMe) {
        AuthorResponse authorResponse = new AuthorResponse(
                row.getAuthorId(), row.getAuthorUsername(), row.getAuthorDisplayName(), row.getAuthorAvatarUrl());
        return new PostResponse(row.getId(), authorResponse, row.getBody(), row.getImageUrl(),
                likeCount, commentCount, isLikedByMe, row.getCreatedAt(), row.getUpdatedAt());
    }
}
