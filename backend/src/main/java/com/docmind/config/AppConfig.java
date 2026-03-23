package com.docmind.config;

import com.docmind.model.Role;
import com.docmind.model.User;
import com.docmind.repository.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AppConfig implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AppConfig.class);

    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AppConfig(JdbcTemplate jdbcTemplate, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        // Enable pgvector extension
        try {
            jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");
            log.info("pgvector extension enabled successfully");
        } catch (Exception e) {
            log.warn("Could not enable pgvector extension: {}", e.getMessage());
        }

        // Create default admin user if no users exist
        if (userRepository.count() == 0) {
            User admin = User.builder()
                    .email("admin@aidoc.com")
                    .passwordHash(passwordEncoder.encode("Admin@123"))
                    .role(Role.ADMIN)
                    .build();
            userRepository.save(admin);
            log.info("Default admin user created: admin@aidoc.com / Admin@123");
        }

        log.info("DocMind platform initialized successfully");
    }
}
