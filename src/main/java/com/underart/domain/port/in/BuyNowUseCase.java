package com.underart.domain.port.in;

import com.underart.domain.model.Order;

public interface BuyNowUseCase {

    Order buyNow(Long auctionId, Long buyerId);
}
