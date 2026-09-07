package com.raisesns.backend.exception;

public class ImageUploadFailedException extends RuntimeException {

    public ImageUploadFailedException(Throwable cause) {
        super("failed to upload image", cause);
    }
}
