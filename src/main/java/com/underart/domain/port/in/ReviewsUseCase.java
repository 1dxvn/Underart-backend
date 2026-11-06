package com.underart.domain.port.in;

import com.underart.domain.model.PageQuery;
import com.underart.domain.model.PageResult;
import com.underart.domain.model.RatingSummary;
import com.underart.domain.model.ReviewView;
import java.util.UUID;

public interface ReviewsUseCase {

    ReviewView create(UUID orderId, UUID reviewerId, int rating, String comment);

    PageResult<ReviewView> listForSeller(UUID sellerId, PageQuery page);

    RatingSummary ratingOf(UUID sellerId);
}
