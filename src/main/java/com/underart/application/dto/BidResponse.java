package com.underart.application.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record BidResponse(Long id, Long auctionId, Long bidderId, BigDecimal amount, Instant placedAt) {}
