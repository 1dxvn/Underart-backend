package com.underart.application.dto;

import com.underart.domain.model.AuctionType;

import java.math.BigDecimal;
import java.time.Instant;

public record CreateArtworkRequest(String title, String description, String imageUrl, AuctionType type,
                                   BigDecimal startingPrice, BigDecimal buyNowPrice, Instant endsAt) {}
