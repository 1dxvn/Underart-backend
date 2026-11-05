package com.underart.infrastructure.persistence.jpa.adapter;

import com.underart.domain.model.ArtworkSearchCriteria;
import com.underart.domain.model.AuctionSearchCriteria;
import com.underart.infrastructure.persistence.jpa.entity.ArtworkEntity;
import com.underart.infrastructure.persistence.jpa.entity.AuctionEntity;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

final class SearchSpecifications {

    private SearchSpecifications() {
    }

    static Specification<ArtworkEntity> artworks(ArtworkSearchCriteria c) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (c.artistId() != null) {
                predicates.add(cb.equal(root.get("artist").get("id"), c.artistId()));
            }
            if (hasText(c.technique())) {
                predicates.add(cb.equal(cb.lower(root.get("technique")), lower(c.technique())));
            }
            if (hasText(c.tag())) {
                Join<ArtworkEntity, String> tags = root.join("tags");
                predicates.add(cb.equal(cb.lower(tags), lower(c.tag())));
                query.distinct(true);
            }
            if (hasText(c.text())) {
                String pattern = "%" + escapeLike(lower(c.text())) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern, '\\'),
                        cb.like(cb.lower(root.get("description")), pattern, '\\')));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    static Specification<AuctionEntity> auctions(AuctionSearchCriteria c) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (c.status() != null) {
                predicates.add(cb.equal(root.get("status"), c.status()));
            }
            if (c.type() != null) {
                predicates.add(cb.equal(root.get("type"), c.type()));
            }
            if (c.artworkId() != null) {
                predicates.add(cb.equal(root.get("artwork").get("id"), c.artworkId()));
            }
            if (c.artistId() != null) {
                predicates.add(cb.equal(root.get("artwork").get("artist").get("id"), c.artistId()));
            }
            if (c.endingBefore() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("endDate"), c.endingBefore()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String lower(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
