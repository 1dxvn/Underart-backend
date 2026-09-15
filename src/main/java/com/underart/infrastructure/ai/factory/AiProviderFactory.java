package com.underart.infrastructure.ai.factory;

import com.underart.domain.port.out.AiProviderPort;
import com.underart.infrastructure.ai.strategy.GroqAiAdapter;
import com.underart.infrastructure.ai.strategy.OpenAiAdapter;
import org.springframework.stereotype.Component;

@Component
public class AiProviderFactory {

    private final GroqAiAdapter groq;
    private final OpenAiAdapter openAi;

    public AiProviderFactory(GroqAiAdapter groq, OpenAiAdapter openAi) {
        this.groq = groq;
        this.openAi = openAi;
    }

    public AiProviderPort getPrimary() {
        return groq;
    }

    public AiProviderPort getFallback() {
        return openAi;
    }
}
