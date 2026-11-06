package com.underart.infrastructure.web.controller;

import com.underart.application.dto.ArtworkMediaResponse;
import com.underart.application.dto.ConfirmMediaRequest;
import com.underart.application.dto.MediaUploadUrlRequest;
import com.underart.application.dto.MediaUploadUrlResponse;
import com.underart.application.dto.ReorderMediaRequest;
import com.underart.domain.port.in.DeleteArtworkMediaUseCase;
import com.underart.domain.port.in.DirectMediaUploadUseCase;
import com.underart.domain.port.in.ListArtworkMediaUseCase;
import com.underart.domain.port.in.ReorderArtworkMediaUseCase;
import com.underart.domain.port.in.UploadArtworkMediaUseCase;
import com.underart.domain.port.in.UploadArtworkMediaUseCase.UploadMediaCommand;
import com.underart.domain.exception.BusinessRuleException;
import com.underart.infrastructure.security.CurrentUserProvider;
import jakarta.validation.Valid;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Tag(name = "Multimedia", description = "Imágenes y videos de las obras en Cloudflare R2")
@RequestMapping("/api/v1/artworks/{artworkId}/media")
@RequiredArgsConstructor
public class ArtworkMediaController {

    private final CurrentUserProvider currentUser;
    private final UploadArtworkMediaUseCase uploadUseCase;
    private final DirectMediaUploadUseCase directUploadUseCase;
    private final ListArtworkMediaUseCase listUseCase;
    private final DeleteArtworkMediaUseCase deleteUseCase;
    private final ReorderArtworkMediaUseCase reorderUseCase;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Subir una imagen o un video corto (multipart, campo file)")
    public ResponseEntity<ArtworkMediaResponse> upload(@PathVariable UUID artworkId,
                                                       @RequestPart("file") MultipartFile file) {
        try (InputStream content = file.getInputStream()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(ArtworkMediaResponse.from(
                    uploadUseCase.upload(new UploadMediaCommand(artworkId, currentUser.currentUserId(),
                            file.getContentType(), file.getSize(), content))));
        } catch (IOException e) {
            throw new BusinessRuleException("No se pudo leer el archivo enviado");
        }
    }

    @GetMapping
    @Operation(summary = "Listar los archivos de una obra; el primero es la portada")
    @SecurityRequirements
    public ResponseEntity<List<ArtworkMediaResponse>> list(@PathVariable UUID artworkId) {
        return ResponseEntity.ok(listUseCase.list(artworkId).stream().map(ArtworkMediaResponse::from).toList());
    }

    @PostMapping("/upload-url")
    @Operation(summary = "Pedir una URL firmada para subir un video grande directo a R2")
    public ResponseEntity<MediaUploadUrlResponse> requestUploadUrl(@PathVariable UUID artworkId,
                                                                   @Valid @RequestBody MediaUploadUrlRequest req) {
        return ResponseEntity.ok(MediaUploadUrlResponse.from(directUploadUseCase.requestUpload(
                artworkId, currentUser.currentUserId(), req.contentType(), req.sizeBytes())));
    }

    @PostMapping("/{mediaId}/confirm")
    @Operation(summary = "Confirmar una subida directa")
    public ResponseEntity<ArtworkMediaResponse> confirm(@PathVariable UUID artworkId, @PathVariable UUID mediaId,
                                                        @Valid @RequestBody ConfirmMediaRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ArtworkMediaResponse.from(
                directUploadUseCase.confirmUpload(artworkId, currentUser.currentUserId(), mediaId, req.contentType())));
    }

    @PutMapping("/order")
    @Operation(summary = "Reordenar los archivos (el primero es la portada)")
    public ResponseEntity<List<ArtworkMediaResponse>> reorder(@PathVariable UUID artworkId,
                                                              @Valid @RequestBody ReorderMediaRequest req) {
        return ResponseEntity.ok(reorderUseCase.reorder(artworkId, currentUser.currentUserId(), req.mediaIds())
                .stream().map(ArtworkMediaResponse::from).toList());
    }

    @DeleteMapping("/{mediaId}")
    @Operation(summary = "Borrar un archivo")
    public ResponseEntity<Void> delete(@PathVariable UUID artworkId, @PathVariable UUID mediaId) {
        deleteUseCase.delete(artworkId, mediaId, currentUser.currentUserId());
        return ResponseEntity.noContent().build();
    }
}
