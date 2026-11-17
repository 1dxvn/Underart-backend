package com.underart.domain.port.in;

import com.underart.domain.model.SellerStats;
import java.util.UUID;

public interface SellerStatsUseCase {

    SellerStats statsFor(UUID userId);
}
