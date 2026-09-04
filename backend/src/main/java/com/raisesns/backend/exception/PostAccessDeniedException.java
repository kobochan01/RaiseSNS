package com.raisesns.backend.exception;

public class PostAccessDeniedException extends RuntimeException {

    public PostAccessDeniedException() {
        super("you do not have permission to modify this post");
    }
}
