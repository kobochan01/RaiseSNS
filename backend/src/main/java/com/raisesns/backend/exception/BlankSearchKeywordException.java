package com.raisesns.backend.exception;

public class BlankSearchKeywordException extends RuntimeException {

    public BlankSearchKeywordException() {
        super("search keyword must not be blank");
    }
}
