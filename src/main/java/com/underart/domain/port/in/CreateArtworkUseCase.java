package com.underart.domain.port.in;

import com.underart.domain.model.Artwork;
import com.underart.domain.model.AuctionType;

import java.math.BigDecimal;
import java.time.Instant;

public interface CreateArtworkUseCase {

    Artwork create(Command command);

    record Command(Long artistId, String title, String description, String imageUrl,
                   AuctionType type, BigDecimal startingPrice, BigDecimal buyNowPrice, Instant endsAt) {}
}
