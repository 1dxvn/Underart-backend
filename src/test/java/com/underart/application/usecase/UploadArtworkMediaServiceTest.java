package com.underart.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.underart.domain.exception.BusinessRuleException;
import com.underart.domain.exception.ForbiddenOperationException;
import com.underart.domain.model.ArtworkMedia;
import com.underart.domain.model.MediaKind;
import com.underart.domain.model.MediaPolicy;
import com.underart.domain.port.in.UploadArtworkMediaUseCase.UploadMediaCommand;
import com.underart.domain.port.out.ArtworkMediaRepositoryPort;
import com.underart.domain.port.out.ArtworkRepositoryPort;
import com.underart.domain.port.out.MediaStoragePort;
import java.io.ByteArrayInputStream;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UploadArtworkMediaServiceTest {

    private final ArtworkRepositoryPort artworks = mock(ArtworkRepositoryPort.class);
    private final ArtworkMediaRepositoryPort media = mock(ArtworkMediaRepositoryPort.class);
    private final MediaStoragePort storage = mock(MediaStoragePort.class);
    private final UploadArtworkMediaService service = new UploadArtworkMediaService(artworks, media, storage);
    private final UUID artistId = UUID.randomUUID();
    private UUID artworkId;

    @BeforeEach
    void setUp() {
        artworkId = Fixtures.artworkOf(artworks, artistId);
        when(storage.publicUrl(anyString())).thenAnswer(i -> "https://cdn.test/" + i.getArgument(0));
        when(media.save(any(ArtworkMedia.class))).thenAnswer(i -> i.getArgument(0));
    }

    private UploadMediaCommand cmd(UUID requester, String type, long size) {
        return new UploadMediaCommand(artworkId, requester, type, size, new ByteArrayInputStream(new byte[]{1}));
    }

    @Test
    void uploadsImageAndStoresMetadata() {
        when(media.countByArtworkId(artworkId)).thenReturn(2L);
        ArtworkMedia saved = service.upload(cmd(artistId, "image/png", 1024));
        assertThat(saved.kind()).isEqualTo(MediaKind.IMAGE);
        assertThat(saved.position()).isEqualTo(2);
        assertThat(saved.storageKey()).startsWith("artworks/" + artworkId + "/").endsWith(".png");
        assertThat(saved.url()).isEqualTo("https://cdn.test/" + saved.storageKey());
        verify(storage).upload(eq(saved.storageKey()), any(), eq(1024L), eq("image/png"));
    }

    @Test
    void rejectsNonOwner() {
        assertThatThrownBy(() -> service.upload(cmd(UUID.randomUUID(), "image/png", 1024)))
                .isInstanceOf(ForbiddenOperationException.class);
        verifyNoInteractions(storage);
    }

    @Test
    void rejectsDisallowedType() {
        assertThatThrownBy(() -> service.upload(cmd(artistId, "application/pdf", 1024)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void rejectsWhenArtworkIsFull() {
        when(media.countByArtworkId(artworkId)).thenReturn((long) MediaPolicy.MAX_MEDIA_PER_ARTWORK);
        assertThatThrownBy(() -> service.upload(cmd(artistId, "image/jpeg", 1024)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void removesStoredFileIfMetadataSaveFails() {
        when(media.save(any(ArtworkMedia.class))).thenThrow(new IllegalStateException("db down"));
        assertThatThrownBy(() -> service.upload(cmd(artistId, "image/webp", 2048)))
                .isInstanceOf(IllegalStateException.class);
        verify(storage).upload(anyString(), any(), anyLong(), anyString());
        verify(storage).delete(anyString());
    }
}
