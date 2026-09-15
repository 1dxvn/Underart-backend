package com.underart.infrastructure.ai.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.underart.domain.exception.AiProviderException;
import com.underart.infrastructure.ai.template.AbstractAiAdapter;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GroqAiAdapter extends AbstractAiAdapter {

    private static final String URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL = "llama-3.1-70b-versatile";
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${ai.groq.api-key}")
    private String apiKey;

    @Override
    protected String buildUrl() {
        return URL;
    }

    @Override
    protected Object buildPayload(String prompt) {
        return Map.of("model", MODEL,
                "messages", List.of(Map.of("role", "user", "content", prompt)));
    }

    @Override
    protected String parseResponse(String raw) {
        try {
            return mapper.readTree(raw).at("/choices/0/message/content").asText();
        } catch (Exception e) {
            throw new AiProviderException("Respuesta de Groq inválida: " + e.getMessage());
        }
    }

    @Override
    protected String getApiKey() {
        return apiKey;
    }

    @Override
    public String getProviderName() {
        return "groq";
    }
}
