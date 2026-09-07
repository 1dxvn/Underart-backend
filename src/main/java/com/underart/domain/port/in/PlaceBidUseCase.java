package com.underart.domain.port.in;

import com.underart.domain.model.Bid;

import java.math.BigDecimal;

public interface PlaceBidUseCase {

    Bid placeBid(Long auctionId, Long bidderId, BigDecimal amount);
}
