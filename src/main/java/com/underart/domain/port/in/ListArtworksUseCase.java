package com.underart.domain.port.in;

import com.underart.domain.model.Artwork;
import com.underart.domain.model.ArtworkSearchCriteria;
import com.underart.domain.model.PageQuery;
import com.underart.domain.model.PageResult;

public interface ListArtworksUseCase {

    PageResult<Artwork> list(ArtworkSearchCriteria criteria, PageQuery page);
}
