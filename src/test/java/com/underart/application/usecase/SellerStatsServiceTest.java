package com.underart.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.underart.domain.model.RatingSummary;
import com.underart.domain.model.SellerStats;
import com.underart.domain.port.out.ArtworkRepositoryPort;
import com.underart.domain.port.out.AuctionRepositoryPort;
import com.underart.domain.port.out.OrderRepositoryPort;
import com.underart.domain.port.out.OrderRepositoryPort.SalesStats;
import com.underart.domain.port.out.ReviewRepositoryPort;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SellerStatsServiceTest {

    private final ArtworkRepositoryPort artworks = mock(ArtworkRepositoryPort.class);
    private final AuctionRepositoryPort auctions = mock(AuctionRepositoryPort.class);
    private final OrderRepositoryPort orders = mock(OrderRepositoryPort.class);
    private final ReviewRepositoryPort reviews = mock(ReviewRepositoryPort.class);
    private final SellerStatsService service = new SellerStatsService(artworks, auctions, orders, reviews);

    @Test
    void combinesCountsRevenueAndRating() {
        UUID userId = UUID.randomUUID();
        when(artworks.countByArtistId(userId)).thenReturn(4L);
        when(auctions.countActiveByArtistId(userId)).thenReturn(2L);
        when(orders.salesStatsForSeller(userId)).thenReturn(new SalesStats(3, new BigDecimal("900.00")));
        when(orders.countByWinnerId(userId)).thenReturn(1L);
        when(reviews.summaryForSeller(userId)).thenReturn(RatingSummary.of(4.5, 2));

        SellerStats stats = service.statsFor(userId);

        assertThat(stats.artworks()).isEqualTo(4);
        assertThat(stats.activeAuctions()).isEqualTo(2);
        assertThat(stats.salesCount()).isEqualTo(3);
        assertThat(stats.revenue()).isEqualByComparingTo("900.00");
        assertThat(stats.purchasesCount()).isEqualTo(1);
        assertThat(stats.rating().average()).isEqualByComparingTo("4.50");
    }
}
