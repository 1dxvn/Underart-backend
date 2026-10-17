package com.underart.application.usecase;

import com.underart.domain.exception.BusinessRuleException;
import com.underart.domain.model.Auction;
import com.underart.domain.model.AuctionType;
import com.underart.domain.model.builder.AuctionBuilder;
import com.underart.domain.port.in.CreateAuctionUseCase;
import com.underart.domain.port.out.ActivityLogRepositoryPort;
import com.underart.domain.port.out.ArtworkRepositoryPort;
import com.underart.domain.port.out.AuctionRepositoryPort;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreateAuctionService implements CreateAuctionUseCase {

    private final AuctionRepositoryPort auctionRepository;
    private final ArtworkRepositoryPort artworkRepository;
    private final ActivityLogRepositoryPort activityLogRepository;

    @Override
    public Auction create(CreateAuctionCommand cmd) {
        ArtworkOwnership.requireOwned(artworkRepository, cmd.artworkId(), cmd.requesterId());
        if (cmd.type() == AuctionType.IMMEDIATE && cmd.immediatePurchasePrice() == null) {
            throw new BusinessRuleException("La compra inmediata requiere un precio de compra inmediata");
        }
        if (cmd.immediatePurchasePrice() != null
                && cmd.immediatePurchasePrice().compareTo(cmd.basePrice()) <= 0) {
            throw new BusinessRuleException("El precio de compra inmediata debe superar el precio base");
        }
        if (auctionRepository.existsActiveByArtworkId(cmd.artworkId())) {
            throw new BusinessRuleException("La obra ya tiene una subasta activa");
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
            throw new BusinessRuleException("La fecha de fin debe ser futura y posterior a la de inicio");
        }
        Auction saved = auctionRepository.save(auction);
        activityLogRepository.log("AUCTION_CREATED", "Subasta creada: " + saved.id(), cmd.requesterId(),
                Map.of("auctionId", saved.id().toString(), "artworkId", saved.artworkId().toString()));
        return saved;
    }
}
