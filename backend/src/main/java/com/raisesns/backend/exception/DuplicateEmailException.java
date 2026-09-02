package com.raisesns.backend.exception;

public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(String email) {
        super("email '" + email + "' is already registered");
    }
}
