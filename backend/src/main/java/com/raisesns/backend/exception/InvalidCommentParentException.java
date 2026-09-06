package com.raisesns.backend.exception;

public class InvalidCommentParentException extends RuntimeException {

    public InvalidCommentParentException(Long parentCommentId) {
        super("parent comment '" + parentCommentId + "' does not belong to this post");
    }
}
