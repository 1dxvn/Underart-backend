package com.underart.application.usecase;

import com.underart.domain.exception.AiProviderException;
import com.underart.domain.model.Artwork;
import com.underart.domain.model.builder.AuctionBuilder;
import com.underart.domain.port.in.CreateArtworkUseCase;
import com.underart.domain.port.out.ArtworkRepositoryPort;
import com.underart.domain.port.out.AuctionRepositoryPort;

public class CreateArtworkService implements CreateArtworkUseCase {

    private static final String FALLBACK_DESCRIPTION = "Obra sin descripción";

    private final ArtworkRepositoryPort artworks;
    private final AuctionRepositoryPort auctions;
    private final ArtworkAiService artworkAi;

    public CreateArtworkService(ArtworkRepositoryPort artworks, AuctionRepositoryPort auctions,
                                ArtworkAiService artworkAi) {
        this.artworks = artworks;
        this.auctions = auctions;
        this.artworkAi = artworkAi;
    }

    @Override
    public Artwork create(Command c) {
        AuctionBuilder auction = AuctionBuilder.anAuction().sellerId(c.artistId()).type(c.type())
                .startingPrice(c.startingPrice()).buyNowPrice(c.buyNowPrice()).endsAt(c.endsAt()).validate();
        Artwork draft = new Artwork(null, c.title(), resolveDescription(c), c.imageUrl(), c.artistId(), null);
        Artwork saved = artworks.save(draft);
        auctions.save(auction.artworkId(saved.id()).build());
        return saved;
    }

    private String resolveDescription(Command c) {
        if (c.description() != null && !c.description().isBlank()) return c.description();
        try {
            return artworkAi.describe(c.title());
        } catch (AiProviderException e) {
            return FALLBACK_DESCRIPTION;
        }
    }
}
