package com.underart.domain.port.out;

import com.underart.domain.model.Auction;
import com.underart.domain.model.AuctionStatus;

import java.util.List;
import java.util.Optional;

public interface AuctionRepositoryPort {

    Auction save(Auction auction);

    Optional<Auction> findById(Long id);

    Optional<Auction> findByIdForUpdate(Long id);

    List<Auction> findByStatus(AuctionStatus status);
}
