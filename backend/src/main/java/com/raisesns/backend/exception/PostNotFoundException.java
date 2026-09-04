package com.raisesns.backend.exception;

public class PostNotFoundException extends RuntimeException {

    public PostNotFoundException(Long postId) {
        super("post '" + postId + "' was not found");
    }
}
