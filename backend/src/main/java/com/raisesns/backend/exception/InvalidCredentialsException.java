package com.raisesns.backend.exception;

public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("メールアドレスまたはパスワードが正しくありません");
    }
}
