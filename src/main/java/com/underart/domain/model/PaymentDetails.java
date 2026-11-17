package com.underart.domain.model;

import java.time.Instant;

public record PaymentDetails(String reference, String token, String transactionId, Instant startedAt) {

    public static PaymentDetails empty() {
        return new PaymentDetails(null, null, null, null);
    }
}
