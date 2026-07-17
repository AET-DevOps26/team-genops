package com.jobready.email.exception;

/**
 * A forged, expired, malformed, or replayed OAuth {@code state} token. Maps to 400 — it signals
 * tampering or a stale link, not a user mid-flow we should bounce back to the frontend.
 */
public class InvalidStateException extends RuntimeException {

    public InvalidStateException(String message) {
        super(message);
    }

    public InvalidStateException(String message, Throwable cause) {
        super(message, cause);
    }
}
