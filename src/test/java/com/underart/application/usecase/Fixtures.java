package com.underart.application.usecase;

import static org.mockito.Mockito.when;

import com.underart.domain.model.Artwork;
import com.underart.domain.model.Auction;
import com.underart.domain.model.AuctionStatus;
import com.underart.domain.model.AuctionType;
import com.underart.domain.port.out.ArtworkRepositoryPort;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

final class Fixtures {

    private Fixtures() {
    }

    static UUID artworkOf(ArtworkRepositoryPort artworks, UUID artistId) {
        UUID artworkId = UUID.randomUUID();
        when(artworks.findById(artworkId)).thenReturn(Optional.of(new Artwork(artworkId, artistId, "Obra",
                "desc", "Óleo", BigDecimal.ONE, "1x1", BigDecimal.TEN, List.of(), Instant.now())));
        return artworkId;
    }

    static Auction auction(UUID artworkId, AuctionStatus status, BigDecimal immediatePrice, Instant end) {
        return new Auction(UUID.randomUUID(), artworkId,
                immediatePrice == null ? AuctionType.SIMPLE : AuctionType.IMMEDIATE, status,
                new BigDecimal("100"), immediatePrice, Instant.now().minusSeconds(7200), end);
    }
}
