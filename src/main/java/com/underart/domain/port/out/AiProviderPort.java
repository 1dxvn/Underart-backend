package com.underart.domain.port.out;

public interface AiProviderPort {

    Completion complete(String prompt);

    record Completion(String provider, String text, long latencyMs) {}
}
