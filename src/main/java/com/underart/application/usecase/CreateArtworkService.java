package com.underart.application.usecase;

import com.underart.domain.model.AiResult;
import com.underart.domain.model.Artwork;
import com.underart.domain.port.in.CreateArtworkUseCase;
import com.underart.domain.port.out.AiGenerationRepositoryPort;
import com.underart.domain.port.out.AiGenerationRepositoryPort.AiGenerationRecord;
import com.underart.domain.port.out.AiProviderPort;
import com.underart.domain.port.out.ArtworkRepositoryPort;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreateArtworkService implements CreateArtworkUseCase {

    private final AiProviderPort aiProvider;
    private final ArtworkRepositoryPort artworkRepository;
    private final AiGenerationRepositoryPort aiGenerationRepository;

    @Override
    public Artwork create(CreateArtworkCommand cmd) {
        AiResult aiResult = aiProvider.valuateArtwork(cmd.title(), cmd.technique(), cmd.dimensions());
        List<String> tags = aiProvider.generateTags(cmd.title(), cmd.description());

        aiGenerationRepository.save(new AiGenerationRecord(aiResult.provider(), cmd.title(),
                aiResult.suggestedPrice(), tags, aiResult.rawJson()));

        Artwork artwork = new Artwork(UUID.randomUUID(), cmd.artistId(), cmd.title(),
                cmd.description(), cmd.technique(), cmd.weightKg(), cmd.dimensions(),
                aiResult.suggestedPrice(), tags, Instant.now());
        return artworkRepository.save(artwork);
    }
}
