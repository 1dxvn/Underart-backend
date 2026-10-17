package com.underart.domain.port.in;

import com.underart.domain.model.Bid;
import com.underart.domain.model.PageQuery;
import com.underart.domain.model.PageResult;
import java.util.UUID;

public interface ListBidsUseCase {

    PageResult<Bid> listByAuction(UUID auctionId, PageQuery page);

    PageResult<Bid> listByBuyer(UUID buyerId, PageQuery page);
}
