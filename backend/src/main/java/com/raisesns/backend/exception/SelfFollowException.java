package com.raisesns.backend.exception;

public class SelfFollowException extends RuntimeException {

    public SelfFollowException() {
        super("you cannot follow yourself");
    }
}
