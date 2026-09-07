package com.underart.application.usecase;

import com.underart.domain.exception.ResourceNotFoundException;
import com.underart.domain.model.Auction;
import com.underart.domain.model.Order;
import com.underart.domain.model.PaymentStatus;
import com.underart.domain.port.in.BuyNowUseCase;
import com.underart.domain.port.out.AuctionRepositoryPort;
import com.underart.domain.port.out.OrderRepositoryPort;

import java.math.BigDecimal;
import java.time.Instant;

public class BuyNowService implements BuyNowUseCase {

    private final AuctionRepositoryPort auctions;
    private final OrderRepositoryPort orders;

    public BuyNowService(AuctionRepositoryPort auctions, OrderRepositoryPort orders) {
        this.auctions = auctions;
        this.orders = orders;
    }

    @Override
    public Order buyNow(Long auctionId, Long buyerId) {
        Instant now = Instant.now();
        Auction auction = auctions.findByIdForUpdate(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subasta", auctionId));
        BigDecimal price = auction.buyNow(buyerId, now);
        Order paid = new Order(null, auctionId, buyerId, price, PaymentStatus.PENDING, now).pay();
        auctions.save(auction);
        return orders.save(paid);
    }
}
