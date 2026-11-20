package com.underart.domain.port.out;

import java.util.Optional;

public interface RegistrationChallengePort {
    boolean replaceWhenReady(String email, String codeHash);
    Optional<String> findValidHashForUpdate(String email);
    void recordFailure(String email);
    void consume(String email);
}
