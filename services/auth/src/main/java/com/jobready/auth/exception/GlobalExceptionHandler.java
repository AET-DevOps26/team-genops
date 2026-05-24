package com.jobready.auth.exception;

import com.jobready.auth.generated.model.Error;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailAlreadyTakenException.class)
    public ResponseEntity<Error> handleEmailTaken(EmailAlreadyTakenException ex) {
        return ResponseEntity.status(409)
            .body(new Error().code("EMAIL_TAKEN").message("Email already registered"));
    }
}
