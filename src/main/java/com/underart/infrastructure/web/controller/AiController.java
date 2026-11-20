package com.underart.infrastructure.web.controller;

import com.underart.application.dto.AiValuationRequest;
import com.underart.application.dto.AiValuationResponse;
import com.underart.application.usecase.ArtworkAiService;
import com.underart.application.usecase.ArtworkAiService.ArtworkAdvice;
import com.underart.domain.model.AuctionCurrency;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import com.underart.infrastructure.security.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "IA", description = "AI artwork valuation")
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final CurrentUserProvider currentUser;
    private final ArtworkAiService artworkAiService;

    public record ArtworkAdviceRequest(@NotBlank @Size(max = 255) String title,
            @Size(max = 2000) String description, @Size(max = 255) String technique,
            @Size(max = 255) String dimensions,
            @Positive @Digits(integer = 6, fraction = 2) BigDecimal weightKg,
            @NotNull AuctionCurrency currency) {}

    @PostMapping("/artwork-advice")
    public ArtworkAdvice advise(@Valid @RequestBody ArtworkAdviceRequest req) {
        return artworkAiService.suggest(currentUser.currentUserId(), req.title(), req.description(),
                req.technique(), req.dimensions(), req.weightKg(), req.currency());
    }

    @PostMapping("/valuations")
    @Operation(summary = "Value artwork without creating it (10 AI requests per hour)")
    public ResponseEntity<AiValuationResponse> valuate(@Valid @RequestBody AiValuationRequest req) {
        return ResponseEntity.ok(AiValuationResponse.from(artworkAiService.processArtwork(
                currentUser.currentUserId(), req.title(), req.description(), req.technique(), req.dimensions())));
    }
}
