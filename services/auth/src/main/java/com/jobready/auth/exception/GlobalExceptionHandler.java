package com.jobready.auth.exception;

import com.jobready.auth.generated.modelDto.Error;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailAlreadyTakenException.class)
    public ResponseEntity<Error> handleEmailTaken(EmailAlreadyTakenException ex) {
        return ResponseEntity.status(409).body(new Error().code("EMAIL_TAKEN").message("Email already registered"));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Error> handleInvalidCredentials(InvalidCredentialsException ex) {
        return ResponseEntity.status(401)
                .body(new Error().code("INVALID_CREDENTIALS").message("Email or password is incorrect"));
    }

    @ExceptionHandler(TooManyAttemptsException.class)
    public ResponseEntity<Error> handleTooManyAttempts(TooManyAttemptsException ex) {
        return ResponseEntity.status(429)
                .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
                .body(new Error().code("TOO_MANY_ATTEMPTS").message("Too many attempts; try again later"));
    }
}
