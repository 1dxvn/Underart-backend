package com.underart.application.usecase;

import com.underart.domain.exception.BusinessRuleException;
import com.underart.domain.exception.ResourceNotFoundException;
import com.underart.domain.model.Auction;
import com.underart.domain.model.builder.AuctionBuilder;
import com.underart.domain.port.out.ArtworkRepositoryPort;
import com.underart.domain.port.out.AuctionRepositoryPort;
import com.underart.domain.port.out.BidRepositoryPort;
import com.underart.domain.port.out.OrderRepositoryPort;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EditAuctionService {

    private final AuctionRepositoryPort auctions;
    private final ArtworkRepositoryPort artworks;
    private final BidRepositoryPort bids;
    private final OrderRepositoryPort orders;

    @Transactional
    public void edit(UUID id, UUID artistId, BigDecimal basePrice, BigDecimal immediatePrice,
            BigDecimal floorPrice, Instant endDate) {
        Auction current = auctions.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found: " + id));
        ArtworkOwnership.requireOwned(artworks, current.artworkId(), artistId);
        Instant now = Instant.now();
        if (!current.isActive() || current.hasEnded(now) || !endDate.isAfter(now)) {
            throw new BusinessRuleException("La subasta debe estar abierta y el nuevo cierre debe ser futuro.");
        }
        if (bids.countByAuctionId(id) > 0 || orders.hasProcessingCheckout(id)) {
            throw new BusinessRuleException("La subasta tiene pujas o un pago en curso y no se puede editar.");
        }
        Auction updated = new AuctionBuilder(id)
                .withArtwork(current.artworkId())
                .withType(current.type())
                .withCurrency(current.currency())
                .withStartDate(current.startDate())
                .withBasePrice(basePrice)
                .withImmediatePurchasePrice(immediatePrice)
                .withFloorPrice(floorPrice)
                .withEndDate(endDate)
                .build();
        auctions.save(updated);
        orders.expireOpenCheckouts(id);
    }
}
