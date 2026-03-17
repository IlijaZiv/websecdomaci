package com.example.websecurity.exception;

public class WebSecForbiddenException extends RuntimeException {
    public WebSecForbiddenException(String message) {
        super(message);
    }
}
