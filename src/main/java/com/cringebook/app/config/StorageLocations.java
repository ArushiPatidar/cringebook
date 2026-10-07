package com.cringebook.app.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Resolves the two directories the app reads and writes at runtime. Both are
 * configurable so the app is not pinned to one developer machine:
 * {@code cringebook.frontend-dir} and {@code cringebook.upload-dir}.
 */
@Component
public class StorageLocations {

    private final Path frontendDir;
    private final Path uploadDir;

    public StorageLocations(@Value("${cringebook.frontend-dir:frontend}") String frontendDir,
                            @Value("${cringebook.upload-dir:uploads}") String uploadDir) {
        this.frontendDir = Paths.get(frontendDir).toAbsolutePath().normalize();
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @PostConstruct
    void createDirectories() throws IOException {
        Files.createDirectories(uploadDir);
    }

    public Path frontendFile(String filename) {
        return resolveWithin(frontendDir, filename);
    }

    public Path uploadFile(String filename) {
        return resolveWithin(uploadDir, filename);
    }

    /**
     * Joins {@code name} onto {@code base} and refuses anything that escapes
     * the base directory. Both the requested page name and uploaded file names
     * arrive from the network, so "../" must never reach the file system.
     */
    private static Path resolveWithin(Path base, String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("empty file name");
        }
        Path resolved = base.resolve(name).normalize();
        if (!resolved.startsWith(base)) {
            throw new IllegalArgumentException("path escapes base directory");
        }
        return resolved;
    }
}
