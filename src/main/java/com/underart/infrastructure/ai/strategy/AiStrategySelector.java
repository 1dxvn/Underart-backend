package com.underart.infrastructure.ai.strategy;

import com.underart.domain.exception.AiProviderException;
import com.underart.domain.model.AiResult;
import com.underart.domain.port.out.ActivityLogRepositoryPort;
import com.underart.domain.port.out.AiProviderPort;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

@Service
@Primary
public class AiStrategySelector implements AiProviderPort {

    private final AiProviderPort primary;
    private final AiProviderPort fallback;
    private final ActivityLogRepositoryPort activityLog;

    public AiStrategySelector(@Qualifier("groqAiAdapter") AiProviderPort primary,
                              @Qualifier("openAiAdapter") AiProviderPort fallback, ActivityLogRepositoryPort activityLog) {
        this.primary = primary;
        this.fallback = fallback;
        this.activityLog = activityLog;
    }

    @Override
    public AiResult valuateArtwork(String title, String technique, String dimensions) {
        return execute(p -> p.valuateArtwork(title, technique, dimensions));
    }

    @Override
    public List<String> generateTags(String title, String description) {
        return execute(p -> p.generateTags(title, description));
    }

    @Override
    public String getProviderName() {
        return primary.getProviderName();
    }

    private <T> T execute(Function<AiProviderPort, T> call) {
        try {
            return call.apply(primary);
        } catch (RestClientException | AiProviderException e) {
            activityLog.log("AI_FALLBACK", primary.getProviderName() + " failed: " + e.getMessage(),
                    null, Map.of("provider", primary.getProviderName()));
        }
        try {
            return call.apply(fallback);
        } catch (RestClientException | AiProviderException e) {
            throw new AiProviderException("All AI providers failed: " + e.getMessage());
        }
    }
}
