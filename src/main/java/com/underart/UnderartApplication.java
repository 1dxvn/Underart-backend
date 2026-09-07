package com.underart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class UnderartApplication {

    public static void main(String[] args) {
        SpringApplication.run(UnderartApplication.class, args);
    }
}
