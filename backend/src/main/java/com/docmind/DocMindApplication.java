package com.docmind;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@SpringBootApplication
public class DocMindApplication {

    public static void main(String[] args) {
        loadDotEnv();
        SpringApplication.run(DocMindApplication.class, args);
    }

    /**
     * Loads .env file into JVM system properties before Spring Boot starts.
     * Spring Boot does not auto-load .env files, so we do it manually here.
     * Looks in parent directory first (project root when running from backend/),
     * then current directory.
     */
    private static void loadDotEnv() {
        List<Path> candidates = List.of(
            Paths.get("../.env"),   // project root when running `mvn` from backend/
            Paths.get(".env"),      // current directory (e.g. when running from project root)
            Paths.get("../../.env") // fallback for nested run dirs
        );

        for (Path envPath : candidates) {
            if (Files.exists(envPath)) {
                try {
                    Files.lines(envPath).forEach(rawLine -> {
                        String line = rawLine.trim();
                        if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) return;
                        int idx = line.indexOf('=');
                        String key = line.substring(0, idx).trim();
                        String value = line.substring(idx + 1).trim();
                        // Strip surrounding quotes if present
                        if (value.length() >= 2 &&
                            ((value.startsWith("\"") && value.endsWith("\"")) ||
                             (value.startsWith("'") && value.endsWith("'")))) {
                            value = value.substring(1, value.length() - 1);
                        }
                        // Only set if not already provided by the OS environment
                        if (System.getenv(key) == null) {
                            System.setProperty(key, value);
                        }
                    });
                    System.out.println("[DocMind] Loaded environment from: "
                        + envPath.toAbsolutePath().normalize());
                } catch (IOException e) {
                    System.err.println("[DocMind] Warning: could not read .env file: " + e.getMessage());
                }
                return; // stop after first match
            }
        }
        System.out.println("[DocMind] No .env file found — relying on OS environment variables");
    }
}
