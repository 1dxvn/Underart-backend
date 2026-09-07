package com.underart.application.dto;

import com.underart.domain.model.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderResponse(Long id, Long auctionId, Long buyerId, BigDecimal amount,
                            PaymentStatus status, Instant createdAt) {}
