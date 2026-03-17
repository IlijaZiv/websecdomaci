package com.example.websecurity.exception;

public class WebSecTooManyAttemptsException extends RuntimeException {
    private final long retryAfterSeconds;

    public WebSecTooManyAttemptsException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
