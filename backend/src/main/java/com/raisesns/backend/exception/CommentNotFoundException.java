package com.raisesns.backend.exception;

public class CommentNotFoundException extends RuntimeException {

    public CommentNotFoundException(Long commentId) {
        super("comment '" + commentId + "' was not found");
    }
}
