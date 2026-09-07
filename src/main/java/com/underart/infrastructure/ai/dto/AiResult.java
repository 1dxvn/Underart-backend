package com.underart.infrastructure.ai.dto;

public record AiResult(String provider, String content, long latencyMs) {}
