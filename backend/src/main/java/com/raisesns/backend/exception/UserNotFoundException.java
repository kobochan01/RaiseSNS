package com.raisesns.backend.exception;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String username) {
        super("user '" + username + "' was not found");
    }
}
