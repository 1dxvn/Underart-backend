package com.underart.application.usecase;

import com.underart.domain.exception.ResourceNotFoundException;
import com.underart.domain.model.Auction;
import com.underart.domain.model.AuctionStatus;
import com.underart.domain.port.in.ListAuctionsUseCase;
import com.underart.domain.port.out.AuctionRepositoryPort;

import java.time.Instant;
import java.util.List;

public class ListAuctionsService implements ListAuctionsUseCase {

    private final AuctionRepositoryPort auctions;

    public ListAuctionsService(AuctionRepositoryPort auctions) {
        this.auctions = auctions;
    }

    @Override
    public List<Auction> listActive() {
        Instant now = Instant.now();
        return auctions.findByStatus(AuctionStatus.ACTIVE).stream().filter(a -> a.isOpen(now)).toList();
    }

    @Override
    public Auction findById(Long id) {
        return auctions.findById(id).orElseThrow(() -> new ResourceNotFoundException("Subasta", id));
    }
}
