package com.underart.application.usecase;

import com.underart.domain.exception.BusinessRuleException;
import com.underart.domain.model.ArtworkMedia;
import com.underart.domain.port.in.DeleteArtworkUseCase;
import com.underart.domain.port.out.ActivityLogRepositoryPort;
import com.underart.domain.port.out.ArtworkMediaRepositoryPort;
import com.underart.domain.port.out.ArtworkRepositoryPort;
import com.underart.domain.port.out.AuctionRepositoryPort;
import com.underart.domain.port.out.MediaStoragePort;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeleteArtworkService implements DeleteArtworkUseCase {

    private final ArtworkRepositoryPort artworkRepository;
    private final AuctionRepositoryPort auctionRepository;
    private final ArtworkMediaRepositoryPort mediaRepository;
    private final MediaStoragePort mediaStorage;
    private final ActivityLogRepositoryPort activityLogRepository;

    @Override
    @Transactional
    public void delete(UUID artworkId, UUID requesterId) {
        ArtworkOwnership.requireOwnedForUpdate(artworkRepository, artworkId, requesterId);
        auctionRepository.deleteCancelledWithoutHistory(artworkId);
        if (auctionRepository.existsByArtworkId(artworkId)) {
            throw new BusinessRuleException("La obra tiene una subasta abierta o un historial de subastas, pujas o pedidos que debe conservarse.");
        }
        for (ArtworkMedia media : mediaRepository.findByArtworkId(artworkId)) {
            mediaStorage.delete(media.storageKey());
            mediaRepository.deleteById(media.id());
        }
        artworkRepository.deleteById(artworkId);
        activityLogRepository.log("ARTWORK_DELETED", "Artwork deleted: " + artworkId, requesterId,
                Map.of("artworkId", artworkId.toString()));
    }
}
