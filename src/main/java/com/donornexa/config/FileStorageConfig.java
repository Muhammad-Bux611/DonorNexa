package com.donornexa.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Portable file storage configuration.
 * Ensures the upload directory is resolved dynamically from environment configuration
 * and automatically initializes the required photo and licence subdirectories.
 */
@Slf4j
@Getter
@Configuration
public class FileStorageConfig {

    @Value("${app.upload.dir:${donornexa.upload.dir:uploads}}")
    private String uploadDir;

    private Path uploadPath;

    @PostConstruct
    public void init() {
        this.uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadPath.resolve("photos"));
            Files.createDirectories(uploadPath.resolve("licences"));
            log.info("Cross-platform file storage initialized at: {}", uploadPath);
        } catch (IOException e) {
            log.error("Could not initialize upload directories at {}: {}", uploadPath, e.getMessage());
        }
    }
}
