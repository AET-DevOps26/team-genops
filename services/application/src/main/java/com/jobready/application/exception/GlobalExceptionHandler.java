package com.jobready.application.exception;

import com.jobready.application.generated.modelDto.Error;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApplicationNotFoundException.class)
    public ResponseEntity<Error> handleNotFound(ApplicationNotFoundException ex) {
        return ResponseEntity.status(404)
            .body(new Error().code("APPLICATION_NOT_FOUND")
            .message(ex.getMessage()));
    }
}
