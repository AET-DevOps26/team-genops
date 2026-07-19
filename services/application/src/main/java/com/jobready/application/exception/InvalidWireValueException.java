package com.jobready.application.exception;

/**
 * A request carried an enum wire value outside the schema (stage, event type). Distinct from
 * {@link IllegalArgumentException} so genuine programming errors keep surfacing as 500s while
 * caller mistakes map to 422 (see {@code GlobalExceptionHandler}).
 */
public class InvalidWireValueException extends RuntimeException {

    public InvalidWireValueException(String message, Throwable cause) {
        super(message, cause);
    }
}
