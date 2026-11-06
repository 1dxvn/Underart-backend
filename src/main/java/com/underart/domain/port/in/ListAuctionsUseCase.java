package com.underart.domain.port.in;

import com.underart.domain.model.AuctionSummary;
import com.underart.domain.model.AuctionSearchCriteria;
import com.underart.domain.model.PageQuery;
import com.underart.domain.model.PageResult;

public interface ListAuctionsUseCase {

    PageResult<AuctionSummary> list(AuctionSearchCriteria criteria, PageQuery page);
}
