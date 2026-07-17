package com.jobready.email.exception;

import com.jobready.email.generated.modelDto.Error;
import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Forged/expired/replayed OAuth state → 400 with the unified error schema. */
    @ExceptionHandler(InvalidStateException.class)
    public ResponseEntity<Error> handleInvalidState(InvalidStateException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new Error().code("BAD_REQUEST").message(ex.getMessage()));
    }

    /** A validly-signed token whose subject isn't a usable user id is a 401, not a 500. */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Error> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new Error().code("UNAUTHORIZED").message(ex.getMessage()));
    }

    /** Missing required query parameter (e.g. callback without code/state). */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Error> handleMissingParameter(MissingServletRequestParameterException ex) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("parameter", ex.getParameterName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new Error()
                        .code("MISSING_PARAMETER")
                        .message("Required request parameter is missing")
                        .details(details));
    }

    /** Bean-validation failure on a query parameter (e.g. limit > 100) — unified error shape. */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Error> handleConstraintViolation(ConstraintViolationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new Error().code("VALIDATION_ERROR").message("Request validation failed"));
    }

    /** A malformed query parameter — same unified error shape. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Error> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("parameter", ex.getName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new Error()
                        .code("INVALID_PARAMETER")
                        .message("Request parameter has an invalid format")
                        .details(details));
    }
}
