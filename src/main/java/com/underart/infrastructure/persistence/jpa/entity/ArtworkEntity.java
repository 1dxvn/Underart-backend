package com.underart.infrastructure.persistence.jpa.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "artworks")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArtworkEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "artist_id")
    private UserEntity artist;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    private String technique;

    private BigDecimal weightKg;

    private String dimensions;

    private BigDecimal suggestedPrice;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "artwork_tags", joinColumns = @JoinColumn(name = "artwork_id"))
    @Column(name = "tag")
    private List<String> tags;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
