package com.jobready.application.exception;

import com.jobready.application.generated.modelDto.Error;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApplicationNotFoundException.class)
    public ResponseEntity<Error> handleNotFound(ApplicationNotFoundException ex) {
        return ResponseEntity.status(404)
            .body(new Error().code("APPLICATION_NOT_FOUND")
            .message(ex.getMessage()));
    }

    /** A validly-signed token whose subject isn't a usable user id is a 401, not a 500. */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Error> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(new Error().code("UNAUTHORIZED").message(ex.getMessage()));
    }

    /**
     * Bad enum wire values (stage, event type) sent by internal callers surface as
     * {@code IllegalArgumentException} from the generated {@code fromValue} — a caller error,
     * not a server error.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Error> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(new Error().code("VALIDATION_ERROR").message(ex.getMessage()));
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
