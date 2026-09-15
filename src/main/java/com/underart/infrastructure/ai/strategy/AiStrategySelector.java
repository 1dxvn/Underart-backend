package com.underart.infrastructure.ai.strategy;

import com.underart.domain.exception.AiProviderException;
import com.underart.domain.model.AiResult;
import com.underart.domain.port.out.ActivityLogRepositoryPort;
import com.underart.domain.port.out.AiProviderPort;
import com.underart.infrastructure.ai.factory.AiProviderFactory;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

@Service
@Primary
public class AiStrategySelector implements AiProviderPort {

    private final AiProviderFactory factory;
    private final ActivityLogRepositoryPort activityLog;

    public AiStrategySelector(AiProviderFactory factory, ActivityLogRepositoryPort activityLog) {
        this.factory = factory;
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
        return factory.getPrimary().getProviderName();
    }

    private <T> T execute(Function<AiProviderPort, T> call) {
        AiProviderPort primary = factory.getPrimary();
        try {
            return call.apply(primary);
        } catch (RestClientException | AiProviderException e) {
            activityLog.log("AI_FALLBACK", primary.getProviderName() + " falló: " + e.getMessage(),
                    null, Map.of("provider", primary.getProviderName()));
        }
        try {
            return call.apply(factory.getFallback());
        } catch (RestClientException | AiProviderException e) {
            throw new AiProviderException("Todos los proveedores de IA fallaron: " + e.getMessage());
        }
    }
}
