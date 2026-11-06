package com.underart.infrastructure.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Estado", description = "Estado general del servicio")
public class RootController {

    @GetMapping("/")
    @Operation(summary = "Comprobar que el servicio está en marcha")
    @SecurityRequirements
    public ResponseEntity<Map<String, String>> root() {
        return ResponseEntity.ok(Map.of(
                "application", "Underart Backend",
                "status", "UP",
                "message", "Underart Backend está funcionando"));
    }
}
