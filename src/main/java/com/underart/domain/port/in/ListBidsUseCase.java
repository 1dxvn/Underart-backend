package com.underart.domain.port.in;

import com.underart.domain.model.Bid;
import java.util.List;
import java.util.UUID;

public interface ListBidsUseCase {

    List<Bid> listByAuction(UUID auctionId);
}
