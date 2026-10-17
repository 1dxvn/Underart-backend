package com.underart.domain.port.in;

import com.underart.domain.model.Auction;
import com.underart.domain.model.AuctionSearchCriteria;
import com.underart.domain.model.PageQuery;
import com.underart.domain.model.PageResult;

public interface ListAuctionsUseCase {

    PageResult<Auction> list(AuctionSearchCriteria criteria, PageQuery page);
}
