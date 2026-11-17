package com.underart.infrastructure.scheduling;

import com.underart.application.usecase.PaymentTransactions;
import com.underart.domain.port.out.OrderRepositoryPort;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderExpirationScheduler {

    private final OrderRepositoryPort orders;
    private final PaymentTransactions payments;

    @Scheduled(fixedDelayString = "${orders.expiration.interval-ms:60000}", initialDelay = 30000)
    public void expire() {
        for (var id : orders.findUnpaidExpiredIds(Instant.now())) {
            try {
                payments.expire(id);
            } catch (RuntimeException e) {
                log.error("Could not expire order {}", id, e);
            }
        }
    }
}
