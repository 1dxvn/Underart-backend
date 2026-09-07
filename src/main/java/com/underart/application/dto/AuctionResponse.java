package com.underart.application.dto;

import com.underart.domain.model.AuctionStatus;
import com.underart.domain.model.AuctionType;

import java.math.BigDecimal;
import java.time.Instant;

public record AuctionResponse(Long id, Long artworkId, Long sellerId, AuctionType type, AuctionStatus status,
                              BigDecimal currentPrice, BigDecimal buyNowPrice, Long highestBidderId, Instant endsAt) {}
