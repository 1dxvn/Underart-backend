package com.underart.application.usecase;

import com.underart.domain.port.out.AiGenerationRepositoryPort;
import com.underart.domain.port.out.AiProviderPort;

public class ArtworkAiService {

    private static final String PROMPT = "Escribe una descripción curatorial breve (máx. 80 palabras) "
            + "para una obra de arte urbano titulada \"%s\".";

    private final AiProviderPort aiProvider;
    private final AiGenerationRepositoryPort generations;

    public ArtworkAiService(AiProviderPort aiProvider, AiGenerationRepositoryPort generations) {
        this.aiProvider = aiProvider;
        this.generations = generations;
    }

    public String describe(String title) {
        String prompt = PROMPT.formatted(title);
        AiProviderPort.Completion completion = aiProvider.complete(prompt);
        generations.save(completion.provider(), prompt, completion.text(), completion.latencyMs());
        return completion.text();
    }
}
