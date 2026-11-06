package com.underart.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.underart.domain.model.Artwork;
import com.underart.domain.model.Auction;
import com.underart.domain.model.AuctionStatus;
import com.underart.domain.model.AuctionSummary;
import com.underart.domain.port.out.ArtworkMediaRepositoryPort;
import com.underart.domain.port.out.ArtworkRepositoryPort;
import com.underart.domain.port.out.BidRepositoryPort;
import com.underart.domain.port.out.BidRepositoryPort.BidStats;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuctionSummaryAssemblerTest {

    private final BidRepositoryPort bids = mock(BidRepositoryPort.class);
    private final ArtworkRepositoryPort artworks = mock(ArtworkRepositoryPort.class);
    private final ArtworkMediaRepositoryPort media = mock(ArtworkMediaRepositoryPort.class);
    private final AuctionSummaryAssembler assembler = new AuctionSummaryAssembler(bids, artworks, media);

    @Test
    void combinesTitlesCoversAndBidStatsInBatch() {
        UUID artworkId = UUID.randomUUID();
        Auction withBids = Fixtures.auction(artworkId, AuctionStatus.ACTIVE, null, Instant.now().plusSeconds(60));
        Auction withoutBids = Fixtures.auction(artworkId, AuctionStatus.CLOSED, null, Instant.now().minusSeconds(60));
        when(bids.statsByAuctionIds(any())).thenReturn(Map.of(withBids.id(), new BidStats(new BigDecimal("250"), 3)));
        when(artworks.findAllByIds(any())).thenReturn(List.of(new Artwork(artworkId, UUID.randomUUID(), "Marina",
                null, null, null, null, null, List.of(), Instant.now())));
        when(media.coverUrlsByArtworkIds(any())).thenReturn(Map.of(artworkId, "https://cdn/x.jpg"));

        List<AuctionSummary> summaries = assembler.summarize(List.of(withBids, withoutBids));

        assertThat(summaries.get(0).currentPrice()).isEqualByComparingTo("250");
        assertThat(summaries.get(0).bidCount()).isEqualTo(3);
        assertThat(summaries.get(0).artworkTitle()).isEqualTo("Marina");
        assertThat(summaries.get(0).coverUrl()).isEqualTo("https://cdn/x.jpg");
        assertThat(summaries.get(1).currentPrice()).isEqualByComparingTo(withoutBids.basePrice());
        assertThat(summaries.get(1).bidCount()).isZero();
    }

    @Test
    void emptyPageDoesNotQuery() {
        assertThat(assembler.summarize(List.of())).isEmpty();
        verifyNoInteractions(bids, artworks, media);
    }
}
