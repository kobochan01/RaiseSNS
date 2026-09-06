package com.raisesns.backend.exception;

public class ProfileAccessDeniedException extends RuntimeException {

    public ProfileAccessDeniedException() {
        super("you do not have permission to modify this profile");
    }
}
