package com.underart.application.usecase;

import com.underart.domain.exception.BusinessRuleException;
import com.underart.domain.exception.ForbiddenOperationException;
import com.underart.domain.exception.ResourceNotFoundException;
import com.underart.domain.model.Artwork;
import com.underart.domain.model.Notification;
import com.underart.domain.model.NotificationType;
import com.underart.domain.model.Order;
import com.underart.domain.model.PageQuery;
import com.underart.domain.model.PageResult;
import com.underart.domain.model.RatingSummary;
import com.underart.domain.model.Review;
import com.underart.domain.model.ReviewView;
import com.underart.domain.model.ShippingStatus;
import com.underart.domain.model.User;
import com.underart.domain.port.in.ReviewsUseCase;
import com.underart.domain.port.out.NotificationSenderPort;
import com.underart.domain.port.out.OrderRepositoryPort;
import com.underart.domain.port.out.ReviewRepositoryPort;
import com.underart.domain.port.out.UserRepositoryPort;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReviewsService implements ReviewsUseCase {

    private final ReviewRepositoryPort reviews;
    private final OrderRepositoryPort orderRepository;
    private final UserRepositoryPort userRepository;
    private final OrderParties orderParties;
    private final NotificationSenderPort notifications;

    @Override
    public ReviewView create(UUID orderId, UUID reviewerId, int rating, String comment) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada: " + orderId));
        if (!order.winnerId().equals(reviewerId)) {
            throw new ForbiddenOperationException("Solo el comprador puede reseñar esta orden");
        }
        if (order.shippingStatus() != ShippingStatus.DELIVERED) {
            throw new BusinessRuleException("Solo puedes reseñar después de confirmar la entrega");
        }
        if (reviews.existsByOrderId(orderId)) {
            throw new BusinessRuleException("Ya reseñaste esta orden");
        }
        Artwork artwork = orderParties.artworkOf(order)
                .orElseThrow(() -> new ResourceNotFoundException("La obra de esta orden ya no existe"));
        Review saved = reviews.save(new Review(UUID.randomUUID(), orderId, reviewerId, artwork.artistId(), rating,
                comment, Instant.now()));
        String reviewer = userRepository.findById(reviewerId).map(User::username).orElse(null);
        notifications.send(Notification.create(artwork.artistId(), NotificationType.NEW_REVIEW, "Recibiste una reseña",
                (reviewer != null ? reviewer : "Un comprador") + " calificó «" + artwork.title() + "» con " + rating
                        + (rating == 1 ? " estrella." : " estrellas."),
                Map.of("orderId", orderId.toString(), "reviewId", saved.id().toString())));
        return new ReviewView(saved, reviewer);
    }

    @Override
    public PageResult<ReviewView> listForSeller(UUID sellerId, PageQuery page) {
        PageResult<Review> found = reviews.findBySeller(sellerId, page);
        Map<UUID, String> usernames = userRepository.findAllByIds(found.items().stream().map(Review::reviewerId)
                .collect(Collectors.toSet())).stream().collect(Collectors.toMap(User::id, User::username));
        return found.map(review -> new ReviewView(review, usernames.get(review.reviewerId())));
    }

    @Override
    public RatingSummary ratingOf(UUID sellerId) {
        return reviews.summaryForSeller(sellerId);
    }
}
