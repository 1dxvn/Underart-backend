package com.underart.domain.model;

import com.underart.domain.exception.BusinessRuleException;
import java.time.Instant;
import java.util.UUID;

public record Review(
        UUID id,
        UUID orderId,
        UUID reviewerId,
        UUID sellerId,
        int rating,
        String comment,
        Instant createdAt) {

    public static final int MIN_RATING = 1;
    public static final int MAX_RATING = 5;
    public static final int MAX_COMMENT_LENGTH = 1000;

    public Review {
        if (rating < MIN_RATING || rating > MAX_RATING) {
            throw new BusinessRuleException("La calificación debe estar entre 1 y 5");
        }
        comment = comment == null || comment.isBlank() ? null : comment.strip();
        if (comment != null && comment.length() > MAX_COMMENT_LENGTH) {
            throw new BusinessRuleException("El comentario no puede superar los 1000 caracteres");
        }
    }
}
