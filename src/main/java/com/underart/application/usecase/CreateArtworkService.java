package com.underart.application.usecase;

import com.underart.domain.exception.AiProviderException;
import com.underart.domain.exception.TooManyRequestsException;
import com.underart.domain.model.AiResult;
import com.underart.domain.model.Artwork;
import com.underart.domain.port.in.CreateArtworkUseCase;
import com.underart.domain.port.out.AiGenerationRepositoryPort.AiGenerationRecord;
import com.underart.domain.port.out.AiGenerationRepositoryPort;
import com.underart.domain.port.out.AiProviderPort;
import com.underart.domain.port.out.ArtworkRepositoryPort;
import com.underart.domain.port.out.RateLimiterPort;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreateArtworkService implements CreateArtworkUseCase {

    private final AiProviderPort aiProvider;
    private final ArtworkRepositoryPort artworkRepository;
    private final AiGenerationRepositoryPort aiGenerationRepository;
    private final RateLimiterPort rateLimiter;

    @Override
    public Artwork create(CreateArtworkCommand cmd) {
        if (!cmd.textValuation()) {
            return saveArtwork(cmd, null, List.of());
        }
        AiResult aiResult = null;
        List<String> tags = List.of();
        try {
            AiUsagePolicy.consume(rateLimiter, cmd.artistId());
            aiResult = aiProvider.valuateArtwork(cmd.title(), cmd.technique(), cmd.dimensions());
            tags = aiResult.tags();
        } catch (AiProviderException
                 | TooManyRequestsException e) {
            log.warn("Artwork created without AI suggestions: {}", e.getMessage());
        }
        if (aiResult != null) {
            try {
                aiGenerationRepository.save(new AiGenerationRecord(aiResult.provider(), cmd.title(),
                        aiResult.suggestedPrice(), tags, aiResult.rawJson()));
            } catch (RuntimeException e) {
                log.warn("Could not record AI generation", e);
            }
        }

        return saveArtwork(cmd, aiResult, tags);
    }

    private Artwork saveArtwork(CreateArtworkCommand cmd, AiResult aiResult, List<String> tags) {
        Artwork artwork = new Artwork(UUID.randomUUID(), cmd.artistId(), cmd.title(),
                cmd.description(), cmd.technique(), cmd.weightKg(), cmd.dimensions(),
                aiResult == null ? null : aiResult.suggestedPrice(), tags, Instant.now());
        return artworkRepository.save(artwork);
    }
}
