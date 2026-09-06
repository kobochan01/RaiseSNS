package com.raisesns.backend.service;

import com.raisesns.backend.dto.response.LikeResponse;
import com.raisesns.backend.exception.PostNotFoundException;
import com.raisesns.backend.mapper.LikeMapper;
import com.raisesns.backend.mapper.PostMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class LikeService {

    private final LikeMapper likeMapper;
    private final PostMapper postMapper;

    public LikeService(LikeMapper likeMapper, PostMapper postMapper) {
        this.likeMapper = likeMapper;
        this.postMapper = postMapper;
    }

    @Transactional
    public LikeResponse like(Long userId, Long postId) {
        postMapper.findById(postId).orElseThrow(() -> new PostNotFoundException(postId));
        likeMapper.insertIfAbsent(postId, userId, LocalDateTime.now());
        return currentState(postId, userId);
    }

    @Transactional
    public LikeResponse unlike(Long userId, Long postId) {
        postMapper.findById(postId).orElseThrow(() -> new PostNotFoundException(postId));
        likeMapper.deleteByPostIdAndUserId(postId, userId);
        return currentState(postId, userId);
    }

    private LikeResponse currentState(Long postId, Long userId) {
        return new LikeResponse(likeMapper.countByPostId(postId), likeMapper.existsByPostIdAndUserId(postId, userId));
    }
}
