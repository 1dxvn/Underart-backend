package com.underart.infrastructure.ai.template;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.underart.domain.exception.AiProviderException;
import com.underart.domain.model.AiResult;
import com.underart.domain.port.out.AiProviderPort;
import java.util.List;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public abstract class AbstractAiAdapter implements AiProviderPort {

    private static final int TIMEOUT_MS = 5000;
    private final ObjectMapper mapper = new ObjectMapper();
    private final RestClient client = buildClient();

    protected abstract String buildUrl();

    protected abstract Object buildPayload(String prompt);

    protected abstract String parseResponse(String raw);

    protected abstract String getApiKey();

    @Override
    public AiResult valuateArtwork(String title, String technique, String dimensions) {
        String prompt = "Valora la obra '" + title + "' (técnica: " + technique + ", dimensiones: "
                + dimensions + "). Responde solo JSON: {\"suggestedPrice\": number, \"tags\": [string]}";
        String content = call(prompt);
        try {
            JsonNode json = mapper.readTree(content);
            return new AiResult(getProviderName(), json.get("suggestedPrice").decimalValue(),
                    mapper.convertValue(json.get("tags"), new TypeReference<List<String>>() {}), content);
        } catch (Exception e) {
            throw new AiProviderException("Respuesta de IA inválida: " + e.getMessage());
        }
    }

    @Override
    public List<String> generateTags(String title, String description) {
        String prompt = "Genera etiquetas para la obra '" + title + "': " + description
                + ". Responde solo un array JSON de strings.";
        try {
            return mapper.readValue(call(prompt), new TypeReference<List<String>>() {});
        } catch (Exception e) {
            throw new AiProviderException("Respuesta de IA inválida: " + e.getMessage());
        }
    }

    private String call(String prompt) {
        try {
            String raw = client.post().uri(buildUrl())
                    .header("Authorization", "Bearer " + getApiKey())
                    .header("Content-Type", "application/json")
                    .body(buildPayload(prompt)).retrieve().body(String.class);
            return parseResponse(raw);
        } catch (AiProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new AiProviderException("Fallo al llamar a " + getProviderName() + ": " + e.getMessage());
        }
    }

    private static RestClient buildClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(TIMEOUT_MS);
        factory.setReadTimeout(TIMEOUT_MS);
        return RestClient.builder().requestFactory(factory).build();
    }
}
