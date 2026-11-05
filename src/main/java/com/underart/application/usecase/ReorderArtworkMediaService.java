package com.underart.application.usecase;

import com.underart.domain.exception.BusinessRuleException;
import com.underart.domain.model.ArtworkMedia;
import com.underart.domain.port.in.ReorderArtworkMediaUseCase;
import com.underart.domain.port.out.ArtworkMediaRepositoryPort;
import com.underart.domain.port.out.ArtworkRepositoryPort;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReorderArtworkMediaService implements ReorderArtworkMediaUseCase {

    private final ArtworkRepositoryPort artworkRepository;
    private final ArtworkMediaRepositoryPort mediaRepository;

    @Override
    public List<ArtworkMedia> reorder(UUID artworkId, UUID requesterId, List<UUID> orderedMediaIds) {
        ArtworkOwnership.requireOwned(artworkRepository, artworkId, requesterId);
        Map<UUID, ArtworkMedia> current = mediaRepository.findByArtworkId(artworkId).stream()
                .collect(Collectors.toMap(ArtworkMedia::id, Function.identity()));
        if (orderedMediaIds.size() != current.size()
                || !new HashSet<>(orderedMediaIds).equals(current.keySet())) {
            throw new BusinessRuleException("Debes enviar todos los archivos de la obra, sin repetir");
        }
        List<ArtworkMedia> reordered = new ArrayList<>();
        for (int i = 0; i < orderedMediaIds.size(); i++) {
            reordered.add(current.get(orderedMediaIds.get(i)).withPosition(i));
        }
        return mediaRepository.saveAll(reordered);
    }
}
