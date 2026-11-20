package com.underart.application.usecase;

import com.underart.domain.model.AiResult;
import com.underart.domain.model.ArtworkDraft;
import com.underart.domain.model.AuctionCurrency;
import com.underart.domain.model.ShippingEstimate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import com.underart.domain.port.out.AiGenerationRepositoryPort.AiGenerationRecord;
import com.underart.domain.port.out.AiGenerationRepositoryPort;
import com.underart.domain.port.out.AiProviderPort;
import com.underart.domain.port.out.RateLimiterPort;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ArtworkAiService {

    private final AiProviderPort aiProvider;
    private final AiGenerationRepositoryPort aiGenerationRepository;
    private final RateLimiterPort rateLimiter;

    public ArtworkAdvice suggest(UUID userId, String title, String notes, String technique, String dimensions,
            BigDecimal weight, AuctionCurrency currency) {
        AiUsagePolicy.consume(rateLimiter, userId);
        ArtworkDraft draft = aiProvider.suggestArtwork(title, notes, technique, dimensions, currency.name());
        return advice(draft, weight, currency);
    }

    static ArtworkAdvice advice(ArtworkDraft draft, BigDecimal weight, AuctionCurrency currency) {
        BigDecimal amount = draft.suggestedPrice();
        if (currency == AuctionCurrency.COP) {
            BigDecimal step = BigDecimal.valueOf(1000);
            amount = amount.divide(step, 0, RoundingMode.CEILING).multiply(step);
        }
        return new ArtworkAdvice(draft.provider(), draft.description(), amount, ShippingEstimate.forWeight(currency.name(), weight), currency);
    }

    public record ArtworkAdvice(String provider, String description, BigDecimal suggestedPrice,
            BigDecimal shippingEstimate, AuctionCurrency currency) {}

    public AiResult processArtwork(UUID requesterId, String title, String description, String technique,
                                   String dimensions) {
        AiUsagePolicy.consume(rateLimiter, requesterId);
        AiResult aiResult = aiProvider.valuateArtwork(title, technique, dimensions);
        List<String> tags = aiProvider.generateTags(title, description);

        aiGenerationRepository.save(new AiGenerationRecord(aiResult.provider(), title,
                aiResult.suggestedPrice(), tags, aiResult.rawJson()));

        return new AiResult(aiResult.provider(), aiResult.suggestedPrice(), tags, aiResult.rawJson());
    }
}
