package com.underart.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.underart.domain.exception.BusinessRuleException;
import com.underart.domain.exception.ForbiddenOperationException;
import com.underart.domain.model.Artwork;
import com.underart.domain.model.Order;
import com.underart.domain.model.PaymentStatus;
import com.underart.domain.model.Review;
import com.underart.domain.model.ReviewView;
import com.underart.domain.model.User;
import com.underart.domain.port.out.NotificationSenderPort;
import com.underart.domain.port.out.OrderRepositoryPort;
import com.underart.domain.port.out.ReviewRepositoryPort;
import com.underart.domain.port.out.UserRepositoryPort;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReviewsServiceTest {

    private final ReviewRepositoryPort reviews = mock(ReviewRepositoryPort.class);
    private final OrderRepositoryPort orders = mock(OrderRepositoryPort.class);
    private final UserRepositoryPort users = mock(UserRepositoryPort.class);
    private final OrderParties parties = mock(OrderParties.class);
    private final NotificationSenderPort notifications = mock(NotificationSenderPort.class);
    private final ReviewsService service = new ReviewsService(reviews, orders, users, parties, notifications);
    private final UUID buyerId = UUID.randomUUID();
    private final UUID sellerId = UUID.randomUUID();
    private Order delivered;

    @BeforeEach
    void setUp() {
        delivered = Order.pending(UUID.randomUUID(), buyerId, new BigDecimal("100.00"))
                .withPaymentStatus(PaymentStatus.PAID).markShipped("DHL", "1", Instant.now()).markDelivered(Instant.now());
        when(orders.findById(delivered.id())).thenReturn(Optional.of(delivered));
        when(parties.artworkOf(delivered)).thenReturn(Optional.of(new Artwork(UUID.randomUUID(), sellerId, "Marina",
                null, null, null, null, null, List.of(), Instant.now())));
        when(reviews.save(any(Review.class))).thenAnswer(i -> i.getArgument(0));
        when(users.findById(buyerId)).thenReturn(Optional.of(
                new User(buyerId, "ana", "ana@test.com", "h", null, null, null, Instant.now())));
    }

    @Test
    void buyerReviewsTheSellerAfterDelivery() {
        ReviewView view = service.create(delivered.id(), buyerId, 5, "Very good");

        assertThat(view.review().sellerId()).isEqualTo(sellerId);
        assertThat(view.review().rating()).isEqualTo(5);
        assertThat(view.reviewerUsername()).isEqualTo("ana");
        verify(notifications).send(org.mockito.ArgumentMatchers.argThat(n -> n.userId().equals(sellerId)
                && n.type() == com.underart.domain.model.NotificationType.NEW_REVIEW
                && n.message().equals("ana calificó «Marina» con 5 estrellas.")));
    }

    @Test
    void cannotReviewTwiceOrBeforeDeliveryOrAsAnotherUser() {
        when(reviews.existsByOrderId(delivered.id())).thenReturn(true);
        assertThatThrownBy(() -> service.create(delivered.id(), buyerId, 5, null))
                .isInstanceOf(BusinessRuleException.class).hasMessage("This order has already been reviewed");
        assertThatThrownBy(() -> service.create(delivered.id(), sellerId, 5, null))
                .isInstanceOf(ForbiddenOperationException.class);

        Order paidOnly = Order.pending(UUID.randomUUID(), buyerId, BigDecimal.TEN).withPaymentStatus(PaymentStatus.PAID);
        when(orders.findById(paidOnly.id())).thenReturn(Optional.of(paidOnly));
        assertThatThrownBy(() -> service.create(paidOnly.id(), buyerId, 5, null))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("confirmed delivery");
        verify(reviews, never()).save(any());
    }
}
