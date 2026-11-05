package com.underart.domain.port.in;

import com.underart.domain.model.Order;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CloseExpiredAuctionsUseCase {

    List<UUID> findExpired();

    Optional<Order> close(UUID auctionId);
}
