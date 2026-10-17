package com.underart.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.underart.domain.exception.BusinessRuleException;
import com.underart.domain.exception.ForbiddenOperationException;
import com.underart.domain.model.Auction;
import com.underart.domain.model.AuctionStatus;
import com.underart.domain.model.AuctionType;
import com.underart.domain.port.in.CreateAuctionUseCase.CreateAuctionCommand;
import com.underart.domain.port.out.ActivityLogRepositoryPort;
import com.underart.domain.port.out.ArtworkRepositoryPort;
import com.underart.domain.port.out.AuctionRepositoryPort;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CreateAuctionServiceTest {

    private final AuctionRepositoryPort auctions = mock(AuctionRepositoryPort.class);
    private final ArtworkRepositoryPort artworks = mock(ArtworkRepositoryPort.class);
    private final ActivityLogRepositoryPort log = mock(ActivityLogRepositoryPort.class);
    private final CreateAuctionService service = new CreateAuctionService(auctions, artworks, log);
    private final UUID artistId = UUID.randomUUID();
    private UUID artworkId;

    @BeforeEach
    void setUp() {
        artworkId = Fixtures.artworkOf(artworks, artistId);
        when(auctions.save(any(Auction.class))).thenAnswer(i -> i.getArgument(0));
    }

    private CreateAuctionCommand cmd(UUID requester, AuctionType type, String immediate, Instant start, Instant end) {
        return new CreateAuctionCommand(requester, artworkId, type, new BigDecimal("100"),
                immediate == null ? null : new BigDecimal(immediate), start, end);
    }

    @Test
    void createsActiveAuctionForOwner() {
        Auction auction = service.create(cmd(artistId, AuctionType.SIMPLE, null, null, Instant.now().plusSeconds(3600)));
        assertThat(auction.status()).isEqualTo(AuctionStatus.ACTIVE);
        assertThat(auction.startDate()).isNotNull();
    }

    @Test
    void rejectsNonOwner() {
        assertThatThrownBy(() -> service.create(cmd(UUID.randomUUID(), AuctionType.SIMPLE, null, null,
                Instant.now().plusSeconds(3600)))).isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void rejectsImmediatePriceNotAboveBase() {
        assertThatThrownBy(() -> service.create(cmd(artistId, AuctionType.IMMEDIATE, "100", null,
                Instant.now().plusSeconds(3600)))).isInstanceOf(BusinessRuleException.class)
                .hasMessage("El precio de compra inmediata debe superar el precio base");
    }

    @Test
    void rejectsSecondActiveAuctionForSameArtwork() {
        when(auctions.existsActiveByArtworkId(artworkId)).thenReturn(true);
        assertThatThrownBy(() -> service.create(cmd(artistId, AuctionType.SIMPLE, null, null,
                Instant.now().plusSeconds(3600)))).isInstanceOf(BusinessRuleException.class)
                .hasMessage("La obra ya tiene una subasta activa");
    }

    @Test
    void rejectsEndBeforeStart() {
        Instant start = Instant.now().plusSeconds(7200);
        assertThatThrownBy(() -> service.create(cmd(artistId, AuctionType.SIMPLE, null, start,
                start.minusSeconds(60)))).isInstanceOf(BusinessRuleException.class);
    }
}
