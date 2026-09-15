package com.underart.infrastructure.persistence.jpa.entity;

import com.underart.domain.model.AuctionStatus;
import com.underart.domain.model.AuctionType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "auctions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuctionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "artwork_id")
    private ArtworkEntity artwork;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuctionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuctionStatus status;

    @Column(nullable = false)
    private BigDecimal basePrice;

    private BigDecimal immediatePurchasePrice;

    @Column(nullable = false)
    private Instant startDate;

    @Column(nullable = false)
    private Instant endDate;
}
