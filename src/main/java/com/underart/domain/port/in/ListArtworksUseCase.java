package com.underart.domain.port.in;

import com.underart.domain.model.ArtworkSummary;
import com.underart.domain.model.ArtworkSearchCriteria;
import com.underart.domain.model.PageQuery;
import com.underart.domain.model.PageResult;

public interface ListArtworksUseCase {

    PageResult<ArtworkSummary> list(ArtworkSearchCriteria criteria, PageQuery page);
}
