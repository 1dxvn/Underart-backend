package com.underart.application.usecase;

import com.underart.domain.exception.BusinessRuleException;
import com.underart.domain.exception.ResourceNotFoundException;
import com.underart.domain.model.Auction;
import com.underart.domain.model.AuctionType;
import com.underart.domain.model.builder.AuctionBuilder;
import com.underart.domain.port.in.CreateAuctionUseCase;
import com.underart.domain.port.out.ArtworkRepositoryPort;
import com.underart.domain.port.out.AuctionRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreateAuctionService implements CreateAuctionUseCase {

    private final AuctionRepositoryPort auctionRepository;
    private final ArtworkRepositoryPort artworkRepository;

    @Override
    public Auction create(CreateAuctionCommand cmd) {
        artworkRepository.findById(cmd.artworkId())
                .orElseThrow(() -> new ResourceNotFoundException("Obra no encontrada"));
        if (cmd.type() == AuctionType.IMMEDIATE && cmd.immediatePurchasePrice() == null) {
            throw new BusinessRuleException("La compra inmediata requiere un precio de compra inmediata");
        }
        Auction auction = new AuctionBuilder()
                .withArtwork(cmd.artworkId())
                .withType(cmd.type())
                .withBasePrice(cmd.basePrice())
                .withImmediatePurchasePrice(cmd.immediatePurchasePrice())
                .withStartDate(cmd.startDate())
                .withEndDate(cmd.endDate())
                .build();
        return auctionRepository.save(auction);
    }
}
