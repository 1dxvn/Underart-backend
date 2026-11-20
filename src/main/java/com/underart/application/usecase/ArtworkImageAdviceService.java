package com.underart.application.usecase;

import com.underart.application.usecase.ArtworkAiService.ArtworkAdvice;
import com.underart.domain.exception.BusinessRuleException;
import com.underart.domain.model.Artwork;
import com.underart.domain.model.AuctionCurrency;
import com.underart.domain.model.MediaKind;
import com.underart.domain.model.MediaPolicy;
import com.underart.domain.port.out.ArtworkMediaRepositoryPort;
import com.underart.domain.port.out.ArtworkRepositoryPort;
import com.underart.domain.port.out.ArtworkVisionPort;
import com.underart.domain.port.out.MediaStoragePort;
import com.underart.domain.port.out.RateLimiterPort;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ArtworkImageAdviceService {

    private static final int MAX_IMAGE_BYTES = 2_900_000;

    private final ArtworkRepositoryPort artworks;
    private final ArtworkMediaRepositoryPort media;
    private final MediaStoragePort storage;
    private final ArtworkVisionPort vision;
    private final RateLimiterPort limiter;

    public ArtworkAdvice suggest(UUID requesterId, UUID artworkId, UUID mediaId, AuctionCurrency currency) {
        Artwork artwork = ArtworkOwnership.requireOwned(artworks, artworkId, requesterId);
        var image = media.findById(mediaId)
                .filter(item -> item.artworkId().equals(artworkId) && item.kind() == MediaKind.IMAGE)
                .orElseThrow(() -> new BusinessRuleException("Elige una imagen de esta obra."));
        AiUsagePolicy.consume(limiter, requesterId);
        if (image.sizeBytes() > MAX_IMAGE_BYTES) {
            throw new BusinessRuleException("Para usar la IA, carga una imagen de hasta 2,9 MB.");
        }
        byte[] bytes = storage.readPrefix(image.storageKey(), MAX_IMAGE_BYTES + 1);
        if (bytes.length == 0 || bytes.length > MAX_IMAGE_BYTES) {
            throw new BusinessRuleException("La imagen para la IA debe pesar entre 1 byte y 2,9 MB.");
        }
        if (!MediaPolicy.hasImageSignature(image.contentType(), bytes)) {
            throw new BusinessRuleException("El archivo no contiene una imagen válida.");
        }
        return ArtworkAiService.advice(vision.analyze(bytes, image.contentType(), currency), artwork.weightKg(), currency);
    }
}
