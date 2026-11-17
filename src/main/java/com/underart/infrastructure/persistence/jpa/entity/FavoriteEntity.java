package com.underart.infrastructure.persistence.jpa.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Entity
@Table(name = "favorites")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteEntity {

    @EmbeddedId
    private FavoriteId id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
