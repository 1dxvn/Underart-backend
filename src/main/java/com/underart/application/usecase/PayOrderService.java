package com.underart.application.usecase;

import com.underart.domain.exception.BusinessRuleException;
import com.underart.domain.exception.ForbiddenOperationException;
import com.underart.domain.exception.PaymentFailedException;
import com.underart.domain.exception.ResourceNotFoundException;
import com.underart.domain.model.Order;
import com.underart.domain.model.PaymentStatus;
import com.underart.domain.model.User;
import com.underart.domain.port.in.FindUserUseCase;
import com.underart.domain.port.in.PayOrderUseCase;
import com.underart.domain.port.out.ActivityLogRepositoryPort;
import com.underart.domain.port.out.OrderRepositoryPort;
import com.underart.domain.port.out.PaymentGatewayPort;
import com.underart.domain.port.out.PaymentGatewayPort.PaymentResult;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PayOrderService implements PayOrderUseCase {

    private final OrderRepositoryPort orderRepository;
    private final FindUserUseCase findUserUseCase;
    private final PaymentGatewayPort paymentGateway;
    private final ActivityLogRepositoryPort activityLogRepository;

    @Override
    public Order pay(UUID orderId, UUID requesterId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada: " + orderId));
        if (!order.winnerId().equals(requesterId)) {
            throw new ForbiddenOperationException("Solo el comprador puede pagar esta orden");
        }
        if (order.paymentStatus() == PaymentStatus.PAID) {
            throw new BusinessRuleException("La orden ya está pagada");
        }
        User buyer = findUserUseCase.findById(requesterId);
        if (buyer.paymentToken() == null || buyer.paymentToken().isBlank()) {
            throw new BusinessRuleException("Registra un método de pago antes de pagar");
        }
        PaymentResult result = paymentGateway.charge(buyer.paymentToken(), order.totalPaid(),
                "order-" + order.id());
        if (!result.approved()) {
            orderRepository.save(order.withPaymentStatus(PaymentStatus.FAILED));
            activityLogRepository.log("PAYMENT_FAILED", "Pago rechazado de la orden " + orderId,
                    requesterId, Map.of("orderId", orderId.toString()));
            throw new PaymentFailedException("Pago rechazado: " + result.message());
        }
        Order paid = orderRepository.save(order.withPaymentStatus(PaymentStatus.PAID));
        activityLogRepository.log("PAYMENT_APPROVED", "Pago aprobado de la orden " + orderId,
                requesterId, Map.of("orderId", orderId.toString(), "transactionId", result.transactionId()));
        return paid;
    }
}
