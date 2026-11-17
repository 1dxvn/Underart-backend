package com.underart.application.usecase;

import com.underart.domain.exception.BusinessRuleException;
import com.underart.domain.port.out.AuctionRepositoryPort;
import com.underart.domain.port.out.OrderRepositoryPort;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ArtworkEditingPolicy {

    private final AuctionRepositoryPort auctions;
    private final OrderRepositoryPort orders;

    public void requireEditable(UUID artworkId) {
        if (auctions.existsActiveByArtworkId(artworkId) || orders.reservesArtwork(artworkId)) {
            throw new BusinessRuleException("Artwork with an active auction or order cannot be changed");
        }
    }
}
