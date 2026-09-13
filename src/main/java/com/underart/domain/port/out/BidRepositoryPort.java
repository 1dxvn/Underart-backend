package com.underart.domain.port.out;

import com.underart.domain.model.Bid;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BidRepositoryPort {

    Bid save(Bid bid);

    List<Bid> findByAuctionId(UUID auctionId);

    Optional<Bid> findHighestBid(UUID auctionId);
}
