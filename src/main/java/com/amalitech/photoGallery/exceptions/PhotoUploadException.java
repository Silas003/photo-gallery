package com.amalitech.photoGallery.exceptions;

public class PhotoUploadException extends RuntimeException {
    public PhotoUploadException(String message, Throwable cause) {
        super(message, cause);
    }
}