package com.underart.domain.model.builder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.underart.domain.model.Auction;
import com.underart.domain.model.AuctionStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuctionBuilderTest {

    @Test
    void buildWithoutStartDateDefaultsToNow() {
        Instant before = Instant.now();
        Auction auction = new AuctionBuilder()
                .withArtwork(UUID.randomUUID())
                .withBasePrice(BigDecimal.TEN)
                .withEndDate(before.plusSeconds(3600))
                .withStartDate(null)
                .build();

        assertThat(auction.startDate()).isNotNull().isAfterOrEqualTo(before);
        assertThat(auction.status()).isEqualTo(AuctionStatus.ACTIVE);
    }

    @Test
    void buildKeepsExplicitStartDate() {
        Instant start = Instant.parse("2030-01-01T00:00:00Z");
        Auction auction = new AuctionBuilder()
                .withArtwork(UUID.randomUUID())
                .withBasePrice(BigDecimal.TEN)
                .withStartDate(start)
                .withEndDate(start.plusSeconds(60))
                .build();

        assertThat(auction.startDate()).isEqualTo(start);
    }

    @Test
    void buildWithoutRequiredFieldsFails() {
        assertThatThrownBy(() -> new AuctionBuilder().build())
                .isInstanceOf(IllegalStateException.class);
    }
}
