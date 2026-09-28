package com.underart.application.facade;

import com.underart.domain.model.Auction;
import com.underart.domain.model.AuctionStatus;
import com.underart.domain.model.Bid;
import com.underart.domain.model.Order;
import com.underart.domain.port.in.BuyNowUseCase;
import com.underart.domain.port.in.ListAuctionsUseCase;
import com.underart.domain.port.in.PlaceBidUseCase;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuctionFacade {

    private final PlaceBidUseCase placeBidUseCase;
    private final BuyNowUseCase buyNowUseCase;
    private final ListAuctionsUseCase listAuctionsUseCase;

    public List<Auction> listActive() {
        return listAuctionsUseCase.list(AuctionStatus.ACTIVE);
    }

    public Bid placeBid(UUID auctionId, UUID buyerId, BigDecimal amount) {
        return placeBidUseCase.place(auctionId, buyerId, amount);
    }

    public Order buyNow(UUID auctionId, UUID buyerId) {
        return buyNowUseCase.buy(auctionId, buyerId);
    }
}
