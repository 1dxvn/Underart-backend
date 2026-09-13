package com.underart.domain.port.out;

import com.underart.domain.model.Auction;
import com.underart.domain.model.AuctionStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuctionRepositoryPort {

    Auction save(Auction auction);

    Optional<Auction> findById(UUID id);

    List<Auction> findByStatus(AuctionStatus status);

    List<Auction> findAll();
}
