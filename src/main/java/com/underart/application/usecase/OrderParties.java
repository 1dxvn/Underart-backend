package com.underart.application.usecase;

import com.underart.domain.model.Artwork;
import com.underart.domain.model.Order;
import com.underart.domain.port.out.ArtworkRepositoryPort;
import com.underart.domain.port.out.AuctionRepositoryPort;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderParties {

    private final AuctionRepositoryPort auctionRepository;
    private final ArtworkRepositoryPort artworkRepository;

    public Optional<Artwork> artworkOf(Order order) {
        return auctionRepository.findById(order.auctionId())
                .flatMap(auction -> artworkRepository.findById(auction.artworkId()));
    }

    public boolean isSeller(Order order, UUID userId) {
        return artworkOf(order).map(artwork -> artwork.artistId().equals(userId)).orElse(false);
    }
}
