package com.raisesns.backend.exception;

public class DuplicateUsernameException extends RuntimeException {

    public DuplicateUsernameException(String username) {
        super("username '" + username + "' is already taken");
    }
}
