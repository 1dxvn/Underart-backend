package com.underart.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.underart.domain.exception.BusinessRuleException;
import com.underart.domain.exception.ForbiddenOperationException;
import com.underart.domain.model.Auction;
import com.underart.domain.model.AuctionStatus;
import com.underart.domain.port.out.ActivityLogRepositoryPort;
import com.underart.domain.port.out.ArtworkRepositoryPort;
import com.underart.domain.port.out.AuctionRepositoryPort;
import com.underart.domain.port.out.BidRepositoryPort;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CancelAuctionServiceTest {

    private final AuctionRepositoryPort auctions = mock(AuctionRepositoryPort.class);
    private final ArtworkRepositoryPort artworks = mock(ArtworkRepositoryPort.class);
    private final BidRepositoryPort bids = mock(BidRepositoryPort.class);
    private final ActivityLogRepositoryPort log = mock(ActivityLogRepositoryPort.class);
    private final CancelAuctionService service = new CancelAuctionService(auctions, artworks, bids, log);
    private final UUID artistId = UUID.randomUUID();
    private Auction auction;

    @BeforeEach
    void setUp() {
        UUID artworkId = Fixtures.artworkOf(artworks, artistId);
        auction = Fixtures.auction(artworkId, AuctionStatus.ACTIVE, null, Instant.now().plusSeconds(3600));
        when(auctions.findByIdForUpdate(auction.id())).thenReturn(Optional.of(auction));
    }

    @Test
    void ownerCancelsAuctionWithoutBids() {
        service.cancel(auction.id(), artistId);
        ArgumentCaptor<Auction> saved = ArgumentCaptor.forClass(Auction.class);
        verify(auctions).save(saved.capture());
        assertThat(saved.getValue().status()).isEqualTo(AuctionStatus.CANCELLED);
    }

    @Test
    void nonOwnerCannotCancel() {
        assertThatThrownBy(() -> service.cancel(auction.id(), UUID.randomUUID()))
                .isInstanceOf(ForbiddenOperationException.class);
        verify(auctions, never()).save(auction);
    }

    @Test
    void cannotCancelAuctionWithBids() {
        when(bids.countByAuctionId(auction.id())).thenReturn(2L);
        assertThatThrownBy(() -> service.cancel(auction.id(), artistId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("No se puede cancelar una subasta que ya tiene pujas");
    }
}
