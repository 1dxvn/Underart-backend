package com.underart.infrastructure.web.controller;

import com.underart.application.dto.AiValuationRequest;
import com.underart.application.dto.AiValuationResponse;
import com.underart.application.usecase.ArtworkAiService;
import com.underart.infrastructure.security.CurrentUserProvider;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "IA", description = "Tasación de obras con inteligencia artificial")
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final CurrentUserProvider currentUser;
    private final ArtworkAiService artworkAiService;

    @PostMapping("/valuations")
    @Operation(summary = "Tasar una obra sin crearla (máximo 10 usos de IA por hora)")
    public ResponseEntity<AiValuationResponse> valuate(@Valid @RequestBody AiValuationRequest req) {
        return ResponseEntity.ok(AiValuationResponse.from(artworkAiService.processArtwork(
                currentUser.currentUserId(), req.title(), req.description(), req.technique(), req.dimensions())));
    }
}
