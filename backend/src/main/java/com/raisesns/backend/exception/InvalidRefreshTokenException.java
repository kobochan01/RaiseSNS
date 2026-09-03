package com.raisesns.backend.exception;

public class InvalidRefreshTokenException extends RuntimeException {

    public InvalidRefreshTokenException() {
        super("リフレッシュトークンが無効です");
    }
}
