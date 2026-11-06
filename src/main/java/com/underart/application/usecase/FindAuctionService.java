package com.underart.application.usecase;

import com.underart.domain.exception.ResourceNotFoundException;
import com.underart.domain.model.Auction;
import com.underart.domain.model.Bid;
import com.underart.domain.model.BidIncrementPolicy;
import com.underart.domain.port.in.FindAuctionUseCase;
import com.underart.domain.port.out.AuctionRepositoryPort;
import com.underart.domain.port.out.BidRepositoryPort;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FindAuctionService implements FindAuctionUseCase {

    private final AuctionRepositoryPort auctionRepository;
    private final BidRepositoryPort bidRepository;
    private final AuctionSummaryAssembler summaryAssembler;

    @Override
    public AuctionDetails findById(UUID id) {
        Auction auction = auctionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subasta no encontrada: " + id));
        Bid highest = bidRepository.findHighestBid(id).orElse(null);
        return new AuctionDetails(summaryAssembler.summarize(auction), highest,
                BidIncrementPolicy.minimumNextBid(auction.basePrice(), highest != null ? highest.amount() : null));
    }
}
