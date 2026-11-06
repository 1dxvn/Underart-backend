package com.underart.domain.port.out;

import com.underart.domain.model.PageQuery;
import com.underart.domain.model.PageResult;
import com.underart.domain.model.RatingSummary;
import com.underart.domain.model.Review;
import java.util.UUID;

public interface ReviewRepositoryPort {

    Review save(Review review);

    boolean existsByOrderId(UUID orderId);

    PageResult<Review> findBySeller(UUID sellerId, PageQuery page);

    RatingSummary summaryForSeller(UUID sellerId);
}
