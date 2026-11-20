package com.underart.infrastructure.persistence.jpa.adapter;

import com.underart.domain.model.ArtworkSearchCriteria;
import com.underart.domain.model.AuctionSearchCriteria;
import com.underart.domain.model.AuctionStatus;
import com.underart.domain.model.PriceRange;
import com.underart.infrastructure.persistence.jpa.entity.ArtworkEntity;
import com.underart.infrastructure.persistence.jpa.entity.AuctionEntity;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Sort;
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
            addRange(predicates, cb, root.get("suggestedPrice"), c.price());
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
            } else if (c.artistId() == null) {
                predicates.add(cb.notEqual(root.get("status"), AuctionStatus.CANCELLED));
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
            addRange(predicates, cb, root.get("basePrice"), c.basePrice());
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    static Sort artworkSort(ArtworkSearchCriteria c) {
        String property = switch (c.sortBy()) {
            case CREATED_AT -> "createdAt";
            case SUGGESTED_PRICE -> "suggestedPrice";
            case TITLE -> "title";
        };
        return Sort.by(c.ascending() ? Sort.Direction.ASC : Sort.Direction.DESC, property).and(Sort.by("id"));
    }

    static Sort auctionSort(AuctionSearchCriteria c) {
        String property = switch (c.sortBy()) {
            case END_DATE -> "endDate";
            case START_DATE -> "startDate";
            case BASE_PRICE -> "basePrice";
        };
        return Sort.by(c.ascending() ? Sort.Direction.ASC : Sort.Direction.DESC, property).and(Sort.by("id"));
    }

    private static void addRange(List<Predicate> predicates, CriteriaBuilder cb, Path<BigDecimal> path,
                                 PriceRange range) {
        if (range.min() != null) {
            predicates.add(cb.greaterThanOrEqualTo(path, range.min()));
        }
        if (range.max() != null) {
            predicates.add(cb.lessThanOrEqualTo(path, range.max()));
        }
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
