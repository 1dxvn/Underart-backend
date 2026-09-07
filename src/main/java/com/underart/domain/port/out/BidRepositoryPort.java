package com.underart.domain.port.out;

import com.underart.domain.model.Bid;

import java.util.List;

public interface BidRepositoryPort {

    Bid save(Bid bid);

    List<Bid> findByAuctionId(Long auctionId);
}
