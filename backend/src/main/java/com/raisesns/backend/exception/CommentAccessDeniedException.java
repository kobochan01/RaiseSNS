package com.raisesns.backend.exception;

public class CommentAccessDeniedException extends RuntimeException {

    public CommentAccessDeniedException() {
        super("you do not have permission to delete this comment");
    }
}
