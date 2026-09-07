package com.underart.application.dto;

import java.time.Instant;

public record ArtworkResponse(Long id, String title, String description, String imageUrl,
                              Long artistId, Instant createdAt) {}
