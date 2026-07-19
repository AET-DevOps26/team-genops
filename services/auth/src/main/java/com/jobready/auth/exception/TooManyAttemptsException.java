package com.jobready.auth.exception;

import lombok.Getter;

/** Thrown when a login lockout or the register throttle is active for the caller. */
@Getter
public class TooManyAttemptsException extends RuntimeException {

    private final long retryAfterSeconds;

    public TooManyAttemptsException(long retryAfterSeconds) {
        super("Too many attempts");
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
