package com.jobready.document.exception;

import com.jobready.document.generated.modelDto.Error;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ProfileNotFoundException.class)
    public ResponseEntity<Error> handleProfileNotFound(ProfileNotFoundException ex) {
        return ResponseEntity.status(404)
            .body(new Error().code("PROFILE_NOT_FOUND")
            .message(ex.getMessage()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Error> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(404)
            .body(new Error().code("RESOURCE_NOT_FOUND")
            .message(ex.getMessage()));
    }

    /** A validly-signed token whose subject isn't a usable user id is a 401, not a 500. */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Error> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(new Error().code("UNAUTHORIZED").message(ex.getMessage()));
    }

    /**
     * Malformed JSON or an invalid enum value in the request body. Spring's default 400 has its
     * own body shape — every error must carry the unified {@code {code, message, details}} schema.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Error> handleUnreadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new Error().code("MALFORMED_REQUEST")
            .message("Request body is malformed or contains an invalid value"));
    }

    /** A non-UUID path or query parameter (e.g. {@code /profile/skills/{id}}) — same reason. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Error> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("parameter", ex.getName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new Error().code("INVALID_PARAMETER")
            .message("Request parameter has an invalid format")
            .details(details));
    }

    /**
     * Bean-validation failures on {@code @Valid} request bodies. The OpenAPI contract promises
     * 422 + the shared Error schema here, so map Spring's default 400 onto that shape.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Error> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, Object> details = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            details.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(new Error().code("VALIDATION_ERROR")
            .message("Request validation failed")
            .details(details));
    }
}
