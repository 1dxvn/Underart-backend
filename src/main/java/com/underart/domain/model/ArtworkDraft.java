package com.underart.domain.model;

import java.math.BigDecimal;

public record ArtworkDraft(String provider, String description, BigDecimal suggestedPrice) {}
