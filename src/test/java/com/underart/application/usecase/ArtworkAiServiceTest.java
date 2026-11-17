package com.underart.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.underart.domain.exception.TooManyRequestsException;
import com.underart.domain.model.AiResult;
import com.underart.domain.port.out.AiGenerationRepositoryPort;
import com.underart.domain.port.out.AiProviderPort;
import com.underart.domain.port.out.RateLimiterPort;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ArtworkAiServiceTest {

    private final AiProviderPort ai = mock(AiProviderPort.class);
    private final AiGenerationRepositoryPort generations = mock(AiGenerationRepositoryPort.class);
    private final RateLimiterPort limiter = mock(RateLimiterPort.class);
    private final ArtworkAiService service = new ArtworkAiService(ai, generations, limiter);
    private final UUID userId = UUID.randomUUID();

    @Test
    void valuatesWithinQuotaAndCountsTheUse() {
        when(limiter.blockedFor(any(), anyInt(), any())).thenReturn(Optional.empty());
        when(ai.valuateArtwork(any(), any(), any()))
                .thenReturn(new AiResult("groq", new BigDecimal("120.00"), List.of(), "{}"));
        when(ai.generateTags(any(), any())).thenReturn(List.of("oil"));

        AiResult result = service.processArtwork(userId, "Portrait", null, "Oil", null);

        assertThat(result.tags()).containsExactly("oil");
        verify(limiter).hit("ai:" + userId, AiUsagePolicy.WINDOW);
    }

    @Test
    void refusesWhenTheHourlyQuotaIsExhausted() {
        when(limiter.blockedFor("ai:" + userId, AiUsagePolicy.MAX_USES_PER_WINDOW, AiUsagePolicy.WINDOW))
                .thenReturn(Optional.of(Duration.ofMinutes(20)));

        assertThatThrownBy(() -> service.processArtwork(userId, "Portrait", null, null, null))
                .isInstanceOf(TooManyRequestsException.class)
                .hasMessageContaining("10 AI requests per hour");
        verifyNoInteractions(ai);
    }
}
