package com.underart.domain.model;

public record AiResult(String provider, java.math.BigDecimal suggestedPrice,
                       java.util.List<String> tags, String rawJson) {}
