package com.underart.domain.port.out;

import com.underart.domain.model.Bid;
import com.underart.domain.model.PageQuery;
import com.underart.domain.model.PageResult;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BidRepositoryPort {

    Bid save(Bid bid);

    List<Bid> findByAuctionId(UUID auctionId);

    PageResult<Bid> findByAuctionId(UUID auctionId, PageQuery page);

    PageResult<Bid> findByBuyerId(UUID buyerId, PageQuery page);

    Optional<Bid> findHighestBid(UUID auctionId);

    long countByAuctionId(UUID auctionId);
}
