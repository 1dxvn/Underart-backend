package com.underart.application.usecase;

import com.underart.domain.exception.BusinessRuleException;
import com.underart.domain.exception.TooManyRequestsException;
import com.underart.domain.model.User;
import com.underart.domain.port.in.RegisterUserUseCase;
import com.underart.domain.port.in.RegisterUserUseCase.RegisterUserCommand;
import com.underart.domain.port.out.PasswordHasherPort;
import com.underart.domain.port.out.RateLimiterPort;
import com.underart.domain.port.out.UserRepositoryPort;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Locale;
import org.springframework.stereotype.Service;
import com.underart.domain.port.out.RegistrationChallengePort;
import com.underart.domain.port.out.VerificationMailPort;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegistrationVerificationService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Duration SEND_WINDOW = Duration.ofMinutes(15);
    private final RegistrationChallengePort challenges;
    private final VerificationMailPort mail;
    private final PasswordHasherPort hasher;
    private final RateLimiterPort limiter;
    private final UserRepositoryPort users;
    private final RegisterUserUseCase registration;

    @Transactional
    public void send(String address, String source) {
        String email = EmailNormalizer.normalize(address);
        String key = "registration-code:" + source;
        limiter.blockedFor(key, 20, SEND_WINDOW).ifPresent(wait -> {
            throw new TooManyRequestsException("Try requesting a code again later", wait.toSeconds());
        });
        limiter.hit(key, SEND_WINDOW);
        if (users.existsByEmail(email)) return;

        String code = String.format(Locale.ROOT, "%06d", RANDOM.nextInt(1_000_000));
        if (challenges.replaceWhenReady(email, hasher.hash(code))) mail.sendCode(email, code);
    }

    @Transactional(noRollbackFor = BusinessRuleException.class)
    public User register(RegisterUserCommand command, String code) {
        String email = EmailNormalizer.normalize(command.email());
        String hash = challenges.findValidHashForUpdate(email)
                .orElseThrow(() -> new BusinessRuleException("Request a new verification code"));
        if (!hasher.matches(code, hash)) {
            challenges.recordFailure(email);
            throw new BusinessRuleException("Invalid verification code");
        }
        User user = registration.register(command);
        challenges.consume(email);
        return user;
    }
}
