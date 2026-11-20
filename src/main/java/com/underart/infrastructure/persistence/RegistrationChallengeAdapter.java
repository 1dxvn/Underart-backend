package com.underart.infrastructure.persistence;

import com.underart.domain.port.out.RegistrationChallengePort;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RegistrationChallengeAdapter implements RegistrationChallengePort {
    private final JdbcTemplate jdbc;

    @Override
    public boolean replaceWhenReady(String email, String codeHash) {
        jdbc.update("delete from registration_challenges where expires_at < now() - interval '1 day'");
        return jdbc.update("""
                insert into registration_challenges (email, code_hash, expires_at, resend_at, attempts)
                values (?, ?, now() + interval '10 minutes', now() + interval '1 minute', 0)
                on conflict (email) do update set code_hash = excluded.code_hash,
                    expires_at = excluded.expires_at, resend_at = excluded.resend_at, attempts = 0
                where registration_challenges.resend_at <= now()
                """, email, codeHash) != 0;
    }

    @Override
    public Optional<String> findValidHashForUpdate(String email) {
        return jdbc.query("""
                select code_hash from registration_challenges
                where email = ? and expires_at > now() and attempts < 5 for update
                """, (row, index) -> row.getString(1), email).stream().findFirst();
    }

    @Override
    public void recordFailure(String email) {
        jdbc.update("update registration_challenges set attempts = attempts + 1 where email = ?", email);
    }

    @Override
    public void consume(String email) {
        jdbc.update("delete from registration_challenges where email = ?", email);
    }
}
