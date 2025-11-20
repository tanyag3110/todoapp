package com.example.auth.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of(
                        "error", "INVALID_CREDENTIALS",
                        "message", ex.getMessage()
                ));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<?> handleIllegalState(IllegalStateException ex) {

        String type =
                ex.getMessage().equals("Account locked") ? "ACCOUNT_LOCKED"
                        : ex.getMessage().equals("Account not confirmed") ? "ACCOUNT_NOT_CONFIRMED"
                        : "ERROR";

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of(
                        "error", type,
                        "message", ex.getMessage()
                ));
    }
}
