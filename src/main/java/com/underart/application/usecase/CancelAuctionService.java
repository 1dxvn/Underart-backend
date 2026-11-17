package com.underart.application.usecase;

import com.underart.domain.exception.BusinessRuleException;
import com.underart.domain.exception.ResourceNotFoundException;
import com.underart.domain.model.Auction;
import com.underart.domain.model.AuctionStatus;
import com.underart.domain.port.in.CancelAuctionUseCase;
import com.underart.domain.port.out.ActivityLogRepositoryPort;
import com.underart.domain.port.out.ArtworkRepositoryPort;
import com.underart.domain.port.out.AuctionRepositoryPort;
import com.underart.domain.port.out.BidRepositoryPort;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CancelAuctionService implements CancelAuctionUseCase {

    private final AuctionRepositoryPort auctionRepository;
    private final ArtworkRepositoryPort artworkRepository;
    private final BidRepositoryPort bidRepository;
    private final ActivityLogRepositoryPort activityLogRepository;

    @Override
    @Transactional
    public void cancel(UUID auctionId, UUID requesterId) {
        Auction auction = auctionRepository.findByIdForUpdate(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found: " + auctionId));
        ArtworkOwnership.requireOwned(artworkRepository, auction.artworkId(), requesterId);
        if (!auction.isActive()) {
            throw new BusinessRuleException("Only active auctions can be cancelled");
        }
        if (bidRepository.countByAuctionId(auctionId) > 0) {
            throw new BusinessRuleException("Auctions with bids cannot be cancelled");
        }
        auctionRepository.save(auction.withStatus(AuctionStatus.CANCELLED));
        activityLogRepository.log("AUCTION_CANCELLED", "Auction cancelled: " + auctionId, requesterId,
                Map.of("auctionId", auctionId.toString()));
    }
}
