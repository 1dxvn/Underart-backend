package com.underart.application.facade;

import com.underart.application.dto.AuctionResponse;
import com.underart.application.dto.BidRequest;
import com.underart.application.dto.BidResponse;
import com.underart.application.dto.OrderResponse;
import com.underart.application.mapper.AuctionDtoMapper;
import com.underart.domain.port.in.BuyNowUseCase;
import com.underart.domain.port.in.ListAuctionsUseCase;
import com.underart.domain.port.in.PlaceBidUseCase;
import com.underart.domain.port.out.BidRepositoryPort;
import com.underart.domain.port.out.OrderRepositoryPort;

import java.util.List;

public class AuctionFacade {

    private final ListAuctionsUseCase listAuctions;
    private final PlaceBidUseCase placeBid;
    private final BuyNowUseCase buyNow;
    private final BidRepositoryPort bids;
    private final OrderRepositoryPort orders;

    public AuctionFacade(ListAuctionsUseCase listAuctions, PlaceBidUseCase placeBid, BuyNowUseCase buyNow,
                         BidRepositoryPort bids, OrderRepositoryPort orders) {
        this.listAuctions = listAuctions;
        this.placeBid = placeBid;
        this.buyNow = buyNow;
        this.bids = bids;
        this.orders = orders;
    }

    public List<AuctionResponse> activeAuctions() {
        return listAuctions.listActive().stream().map(AuctionDtoMapper::toResponse).toList();
    }

    public AuctionResponse auction(Long id) {
        return AuctionDtoMapper.toResponse(listAuctions.findById(id));
    }

    public BidResponse placeBid(Long auctionId, Long bidderId, BidRequest request) {
        return AuctionDtoMapper.toResponse(placeBid.placeBid(auctionId, bidderId, request.amount()));
    }

    public List<BidResponse> bidsOf(Long auctionId) {
        listAuctions.findById(auctionId);
        return bids.findByAuctionId(auctionId).stream().map(AuctionDtoMapper::toResponse).toList();
    }

    public OrderResponse buyNow(Long auctionId, Long buyerId) {
        return AuctionDtoMapper.toResponse(buyNow.buyNow(auctionId, buyerId));
    }

    public List<OrderResponse> ordersOf(Long buyerId) {
        return orders.findByBuyerId(buyerId).stream().map(AuctionDtoMapper::toResponse).toList();
    }
}
