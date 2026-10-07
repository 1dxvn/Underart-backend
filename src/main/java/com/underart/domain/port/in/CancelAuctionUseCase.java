package com.underart.domain.port.in;

import java.util.UUID;

public interface CancelAuctionUseCase {

    void cancel(UUID auctionId);
}
