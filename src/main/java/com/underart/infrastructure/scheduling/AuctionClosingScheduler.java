package com.underart.infrastructure.scheduling;

import com.underart.domain.port.in.CloseExpiredAuctionsUseCase;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionClosingScheduler {

    private final CloseExpiredAuctionsUseCase closeExpiredAuctionsUseCase;

    @Scheduled(fixedDelayString = "${auctions.closing.interval-ms:60000}",
            initialDelayString = "${auctions.closing.initial-delay-ms:30000}")
    public void closeExpiredAuctions() {
        for (UUID auctionId : closeExpiredAuctionsUseCase.findExpired()) {
            try {
                closeExpiredAuctionsUseCase.close(auctionId).ifPresentOrElse(
                        order -> log.info("Subasta {} cerrada con orden {}", auctionId, order.id()),
                        () -> log.info("Subasta {} cerrada sin pujas", auctionId));
            } catch (RuntimeException e) {
                log.error("No se pudo cerrar la subasta {}", auctionId, e);
            }
        }
    }
}
