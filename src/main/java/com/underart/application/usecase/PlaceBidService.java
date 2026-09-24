package com.underart.application.usecase;

import com.underart.domain.exception.BusinessRuleException;
import com.underart.domain.exception.InvalidBidException;
import com.underart.domain.exception.ResourceNotFoundException;
import com.underart.domain.model.Auction;
import com.underart.domain.model.AuctionStatus;
import com.underart.domain.model.Bid;
import com.underart.domain.port.in.PlaceBidUseCase;
import com.underart.domain.port.out.ActivityLogRepositoryPort;
import com.underart.domain.port.out.AuctionRepositoryPort;
import com.underart.domain.port.out.BidRepositoryPort;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlaceBidService implements PlaceBidUseCase {

    private final AuctionRepositoryPort auctionRepository;
    private final BidRepositoryPort bidRepository;
    private final ActivityLogRepositoryPort activityLogRepository;

    @Override
    public Bid place(UUID auctionId, UUID buyerId, BigDecimal amount) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subasta no encontrada: " + auctionId));
        if (auction.status() != AuctionStatus.ACTIVE) {
            throw new BusinessRuleException("La subasta no está activa");
        }
        if (amount.compareTo(auction.basePrice()) <= 0) {
            throw new InvalidBidException("La puja debe superar el precio base");
        }
        bidRepository.findHighestBid(auctionId).ifPresent(highest -> {
            if (amount.compareTo(highest.amount()) <= 0) {
                throw new InvalidBidException("La puja debe superar la puja actual");
            }
        });
        Bid bid = new Bid(UUID.randomUUID(), auctionId, buyerId, amount, Instant.now());
        Bid saved = bidRepository.save(bid);
        activityLogRepository.log("BID_PLACED", "Puja de " + amount + " en subasta " + auctionId,
                buyerId, Map.of("amount", amount));
        return saved;
    }
}
