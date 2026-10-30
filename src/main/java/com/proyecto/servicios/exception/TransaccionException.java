package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public class TransaccionException extends OnboardingException {
    public TransaccionException(String message, HttpStatus status) {
        super(message, status);
    }
}
