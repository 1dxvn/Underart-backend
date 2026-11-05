package com.underart.domain.port.out;

import com.underart.domain.model.Auction;
import com.underart.domain.model.AuctionSearchCriteria;
import com.underart.domain.model.AuctionStatus;
import com.underart.domain.model.PageQuery;
import com.underart.domain.model.PageResult;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuctionRepositoryPort {

    Auction save(Auction auction);

    Optional<Auction> findById(UUID id);

    Optional<Auction> findByIdForUpdate(UUID id);

    List<Auction> findByStatus(AuctionStatus status);

    List<Auction> findAll();

    PageResult<Auction> search(AuctionSearchCriteria criteria, PageQuery page);

    List<UUID> findExpiredActiveIds(Instant now);

    boolean existsByArtworkId(UUID artworkId);

    boolean existsActiveByArtworkId(UUID artworkId);
}
