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
import java.util.List;
import com.underart.domain.model.ArtworkMedia;
import com.underart.domain.model.MediaKind;
import com.underart.domain.port.out.ArtworkMediaRepositoryPort;
import com.underart.domain.port.out.MediaStoragePort;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CreateAuctionServiceTest {

    private final AuctionRepositoryPort auctions = mock(AuctionRepositoryPort.class);
    private final ArtworkRepositoryPort artworks = mock(ArtworkRepositoryPort.class);
    private final ActivityLogRepositoryPort log = mock(ActivityLogRepositoryPort.class);
    private final com.underart.domain.port.out.OrderRepositoryPort orders = mock(com.underart.domain.port.out.OrderRepositoryPort.class);
    private final ArtworkMediaRepositoryPort media = mock(ArtworkMediaRepositoryPort.class);
    private final MediaStoragePort storage = mock(MediaStoragePort.class);
    private final CreateAuctionService service = new CreateAuctionService(auctions, artworks, log, orders, media, storage);
    private final UUID artistId = UUID.randomUUID();
    private UUID artworkId;

    @BeforeEach
    void setUp() {
        artworkId = Fixtures.artworkOf(artworks, artistId);
        var image = new ArtworkMedia(UUID.randomUUID(), artworkId, MediaKind.IMAGE, "https://example.test/cover",
                "cover", "image/jpeg", 30, 0, Instant.now());
        when(media.findByArtworkId(artworkId)).thenReturn(List.of(image));
        when(storage.readPrefix("cover", 12)).thenReturn(new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff});
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
    void rejectsAnArtworkWithoutAnImage() {
        when(media.findByArtworkId(artworkId)).thenReturn(List.of());
        assertThatThrownBy(() -> service.create(cmd(artistId, AuctionType.SIMPLE, null, null, Instant.now().plusSeconds(3600))))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("imagen");
    }

    @Test
    void rejectsVideoMasqueradingAsAnImage() {
        when(storage.readPrefix("cover", 12)).thenReturn(new byte[]{0, 0, 0});
        assertThatThrownBy(() -> service.create(cmd(artistId, AuctionType.SIMPLE, null, null, Instant.now().plusSeconds(3600))))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("imagen");
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
                .hasMessage("The buy-now price must exceed the base price");
    }

    @Test
    void rejectsSecondActiveAuctionForSameArtwork() {
        when(auctions.existsActiveByArtworkId(artworkId)).thenReturn(true);
        assertThatThrownBy(() -> service.create(cmd(artistId, AuctionType.SIMPLE, null, null,
                Instant.now().plusSeconds(3600)))).isInstanceOf(BusinessRuleException.class)
                .hasMessage("This artwork already has an active auction");
    }

    @Test
    void rejectsEndBeforeStart() {
        Instant start = Instant.now().plusSeconds(7200);
        assertThatThrownBy(() -> service.create(cmd(artistId, AuctionType.SIMPLE, null, start,
                start.minusSeconds(60)))).isInstanceOf(BusinessRuleException.class);
    }
}
