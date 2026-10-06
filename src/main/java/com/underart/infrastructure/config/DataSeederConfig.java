package com.underart.infrastructure.config;

import com.underart.domain.model.User;
import com.underart.domain.port.out.UserRepositoryPort;
import java.time.Instant;
import java.util.UUID;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Profile("dev")
public class DataSeederConfig {

    @Bean
    CommandLineRunner seedData(UserRepositoryPort userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.existsByEmail("demo@underart.com")) {
                return;
            }
            User user = new User(UUID.randomUUID(), "demo", "demo@underart.com",
                    passwordEncoder.encode("password123"), "Bogotá", "Calle 100 #10-20",
                    null, Instant.now());
            userRepository.save(user);
            System.out.println("Seeded demo user: demo@underart.com / password123");
        };
    }
}
