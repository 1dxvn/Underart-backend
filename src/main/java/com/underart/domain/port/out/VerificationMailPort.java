package com.underart.domain.port.out;

public interface VerificationMailPort {
    void sendCode(String email, String code);
}
