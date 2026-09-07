package com.underart.domain.port.out;

public interface AiGenerationRepositoryPort {

    void save(String provider, String prompt, String output, long latencyMs);
}
