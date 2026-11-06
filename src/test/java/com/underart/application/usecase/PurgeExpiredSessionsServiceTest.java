package com.underart.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.underart.domain.port.out.RefreshTokenRepositoryPort;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PurgeExpiredSessionsServiceTest {

    private final RefreshTokenRepositoryPort tokens = mock(RefreshTokenRepositoryPort.class);
    private final PurgeExpiredSessionsService service = new PurgeExpiredSessionsService(tokens);

    @Test
    void deletesTokensExpiredBeforeTheRetentionWindow() {
        when(tokens.deleteExpiredBefore(any())).thenReturn(4L);

        assertThat(service.purgeExpired()).isEqualTo(4);

        ArgumentCaptor<Instant> cutoff = ArgumentCaptor.forClass(Instant.class);
        verify(tokens).deleteExpiredBefore(cutoff.capture());
        assertThat(cutoff.getValue()).isCloseTo(Instant.now().minus(PurgeExpiredSessionsService.RETENTION),
                within(5, ChronoUnit.SECONDS));
    }
}
