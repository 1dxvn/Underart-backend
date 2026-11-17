package com.underart.application.usecase;

import com.underart.domain.exception.BusinessRuleException;
import com.underart.domain.model.Auction;
import com.underart.domain.model.AuctionType;
import com.underart.domain.model.builder.AuctionBuilder;
import com.underart.domain.port.in.CreateAuctionUseCase;
import com.underart.domain.port.out.ActivityLogRepositoryPort;
import com.underart.domain.port.out.ArtworkRepositoryPort;
import com.underart.domain.port.out.AuctionRepositoryPort;
import com.underart.domain.port.out.OrderRepositoryPort;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateAuctionService implements CreateAuctionUseCase {

    private final AuctionRepositoryPort auctionRepository;
    private final ArtworkRepositoryPort artworkRepository;
    private final ActivityLogRepositoryPort activityLogRepository;
    private final OrderRepositoryPort orderRepository;

    @Override
    @Transactional
    public Auction create(CreateAuctionCommand cmd) {
        ArtworkOwnership.requireOwnedForUpdate(artworkRepository, cmd.artworkId(), cmd.requesterId());
        if (orderRepository.reservesArtwork(cmd.artworkId())) {
            throw new BusinessRuleException("This artwork is reserved or sold");
        }
        if (cmd.type() == AuctionType.IMMEDIATE && cmd.immediatePurchasePrice() == null) {
            throw new BusinessRuleException("Buy-now auctions require a buy-now price");
        }
        if (cmd.immediatePurchasePrice() != null
                && cmd.immediatePurchasePrice().compareTo(cmd.basePrice()) <= 0) {
            throw new BusinessRuleException("The buy-now price must exceed the base price");
        }
        if (auctionRepository.existsActiveByArtworkId(cmd.artworkId())) {
            throw new BusinessRuleException("This artwork already has an active auction");
        }
        Auction auction = new AuctionBuilder()
                .withArtwork(cmd.artworkId())
                .withType(cmd.type())
                .withBasePrice(cmd.basePrice())
                .withImmediatePurchasePrice(cmd.immediatePurchasePrice())
                .withStartDate(cmd.startDate())
                .withEndDate(cmd.endDate())
                .build();
        if (!auction.endDate().isAfter(auction.startDate()) || !auction.endDate().isAfter(Instant.now())) {
            throw new BusinessRuleException("The end date must be in the future and after the start date");
        }
        Auction saved = auctionRepository.save(auction);
        activityLogRepository.log("AUCTION_CREATED", "Auction created: " + saved.id(), cmd.requesterId(),
                Map.of("auctionId", saved.id().toString(), "artworkId", saved.artworkId().toString()));
        return saved;
    }
}
