package com.underart.domain.exception;

public class VerificationMailException extends RuntimeException {
    public VerificationMailException(Throwable cause) {
        super("Verification email could not be sent", cause);
    }
}
