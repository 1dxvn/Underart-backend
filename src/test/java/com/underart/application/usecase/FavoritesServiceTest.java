package com.underart.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.underart.domain.exception.ResourceNotFoundException;
import com.underart.domain.model.Artwork;
import com.underart.domain.model.ArtworkSummary;
import com.underart.domain.model.PageQuery;
import com.underart.domain.model.PageResult;
import com.underart.domain.port.out.ArtworkMediaRepositoryPort;
import com.underart.domain.port.out.ArtworkRepositoryPort;
import com.underart.domain.port.out.FavoriteRepositoryPort;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FavoritesServiceTest {

    private final FavoriteRepositoryPort favorites = mock(FavoriteRepositoryPort.class);
    private final ArtworkRepositoryPort artworks = mock(ArtworkRepositoryPort.class);
    private final ArtworkMediaRepositoryPort media = mock(ArtworkMediaRepositoryPort.class);
    private final FavoritesService service = new FavoritesService(favorites, artworks, media);
    private final UUID userId = UUID.randomUUID();

    private Artwork artwork(String title) {
        return new Artwork(UUID.randomUUID(), UUID.randomUUID(), title, null, null, null, null, null, List.of(),
                Instant.now());
    }

    @Test
    void addingRequiresAnExistingArtwork() {
        UUID missing = UUID.randomUUID();
        when(artworks.findById(missing)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> service.add(userId, missing)).isInstanceOf(ResourceNotFoundException.class);
        verify(favorites, never()).add(any(), any());
    }

    @Test
    void listKeepsTheFavoriteOrderAndSkipsDeletedArtworks() {
        Artwork newest = artwork("Nueva");
        Artwork oldest = artwork("Vieja");
        UUID deleted = UUID.randomUUID();
        when(favorites.artworkIdsByUser(userId, PageQuery.first()))
                .thenReturn(new PageResult<>(List.of(newest.id(), deleted, oldest.id()), 0, 20, 3));
        when(artworks.findAllByIds(any())).thenReturn(List.of(oldest, newest));
        when(media.coverUrlsByArtworkIds(any())).thenReturn(Map.of(newest.id(), "https://cdn/n.jpg"));

        PageResult<ArtworkSummary> page = service.list(userId, PageQuery.first());

        assertThat(page.items()).extracting(s -> s.artwork().title()).containsExactly("Nueva", "Vieja");
        assertThat(page.items().get(0).coverUrl()).isEqualTo("https://cdn/n.jpg");
        assertThat(page.totalItems()).isEqualTo(3);
    }
}
