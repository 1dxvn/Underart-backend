package com.underart.application.usecase;

import com.underart.domain.model.AiResult;
import com.underart.domain.port.out.AiGenerationRepositoryPort;
import com.underart.domain.port.out.AiGenerationRepositoryPort.AiGenerationRecord;
import com.underart.domain.port.out.AiProviderPort;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ArtworkAiService {

    private final AiProviderPort aiProvider;
    private final AiGenerationRepositoryPort aiGenerationRepository;

    public AiResult processArtwork(String title, String description, String technique, String dimensions) {
        AiResult aiResult = aiProvider.valuateArtwork(title, technique, dimensions);
        List<String> tags = aiProvider.generateTags(title, description);

        aiGenerationRepository.save(new AiGenerationRecord(aiResult.provider(), title,
                aiResult.suggestedPrice(), tags, aiResult.rawJson()));

        return new AiResult(aiResult.provider(), aiResult.suggestedPrice(), tags, aiResult.rawJson());
    }
}
