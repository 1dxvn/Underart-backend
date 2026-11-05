package com.underart.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.underart.domain.exception.BusinessRuleException;
import com.underart.domain.exception.InvalidBidException;
import com.underart.domain.model.Auction;
import com.underart.domain.model.AuctionStatus;
import com.underart.domain.model.AuctionType;
import com.underart.domain.model.Bid;
import com.underart.domain.port.out.ActivityLogRepositoryPort;
import com.underart.domain.port.out.ArtworkRepositoryPort;
import com.underart.domain.port.out.AuctionRepositoryPort;
import com.underart.domain.port.out.BidRepositoryPort;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PlaceBidServiceTest {

    private final AuctionRepositoryPort auctions = mock(AuctionRepositoryPort.class);
    private final BidRepositoryPort bids = mock(BidRepositoryPort.class);
    private final ArtworkRepositoryPort artworks = mock(ArtworkRepositoryPort.class);
    private final ActivityLogRepositoryPort log = mock(ActivityLogRepositoryPort.class);
    private final PlaceBidService service = new PlaceBidService(auctions, bids, artworks, log);
    private final UUID auctionId = UUID.randomUUID();
    private final UUID artistId = UUID.randomUUID();
    private final UUID buyerId = UUID.randomUUID();
    private UUID artworkId;

    private void givenAuction(Instant start, Instant end) {
        artworkId = Fixtures.artworkOf(artworks, artistId);
        Auction auction = new Auction(auctionId, artworkId, AuctionType.SIMPLE, AuctionStatus.ACTIVE,
                new BigDecimal("100"), null, start, end);
        when(auctions.findByIdForUpdate(auctionId)).thenReturn(Optional.of(auction));
    }

    @BeforeEach
    void noPreviousBids() {
        when(bids.findHighestBid(auctionId)).thenReturn(Optional.empty());
        when(bids.save(any(Bid.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void rejectsBidOnEndedAuction() {
        givenAuction(Instant.now().minusSeconds(7200), Instant.now().minusSeconds(60));
        assertThatThrownBy(() -> service.place(auctionId, buyerId, new BigDecimal("150")))
                .isInstanceOf(BusinessRuleException.class).hasMessage("La subasta ya terminó");
        verify(bids, never()).save(any());
    }

    @Test
    void rejectsBidBeforeStart() {
        givenAuction(Instant.now().plusSeconds(600), Instant.now().plusSeconds(7200));
        assertThatThrownBy(() -> service.place(auctionId, buyerId, new BigDecimal("150")))
                .isInstanceOf(BusinessRuleException.class).hasMessage("La subasta aún no ha comenzado");
    }

    @Test
    void rejectsBidOnOwnArtwork() {
        givenAuction(Instant.now().minusSeconds(60), Instant.now().plusSeconds(3600));
        assertThatThrownBy(() -> service.place(auctionId, artistId, new BigDecimal("150")))
                .isInstanceOf(BusinessRuleException.class).hasMessage("No puedes pujar en tu propia obra");
    }

    @Test
    void rejectsBidBelowBasePrice() {
        givenAuction(Instant.now().minusSeconds(60), Instant.now().plusSeconds(3600));
        assertThatThrownBy(() -> service.place(auctionId, buyerId, new BigDecimal("50")))
                .isInstanceOf(InvalidBidException.class);
    }

    @Test
    void rejectsBidNotAboveCurrentHighest() {
        givenAuction(Instant.now().minusSeconds(60), Instant.now().plusSeconds(3600));
        when(bids.findHighestBid(auctionId)).thenReturn(Optional.of(
                new Bid(UUID.randomUUID(), auctionId, UUID.randomUUID(), new BigDecimal("200"), Instant.now())));
        assertThatThrownBy(() -> service.place(auctionId, buyerId, new BigDecimal("200")))
                .isInstanceOf(InvalidBidException.class).hasMessage("La puja debe superar la puja actual");
    }

    @Test
    void acceptsValidBidAndLogsIt() {
        givenAuction(Instant.now().minusSeconds(60), Instant.now().plusSeconds(3600));
        Bid bid = service.place(auctionId, buyerId, new BigDecimal("150"));
        assertThat(bid.amount()).isEqualByComparingTo("150");
        assertThat(bid.buyerId()).isEqualTo(buyerId);
        verify(log).log(any(), any(), any(), any());
    }
}
