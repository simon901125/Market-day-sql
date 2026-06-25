package com.example.demo.Config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class UploadResourceConfig implements WebMvcConfigurer {

    @Value("${app.upload-dir}")
    private String uploadDir;

    @Value("${app.upload-url-prefix:/uploads}")
    private String uploadUrlPrefix;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        createUploadDirectory(uploadPath);

        String urlPattern = normalizeUrlPrefix(uploadUrlPrefix) + "/**";
        String resourceLocation = "file:" + normalizeFileLocation(uploadDir);

        registry.addResourceHandler(urlPattern)
                .addResourceLocations(resourceLocation);

        System.out.println("Upload resources mapped: " + urlPattern + " -> " + resourceLocation);
    }

    private void createUploadDirectory(Path uploadPath) {
        try {
            Files.createDirectories(uploadPath);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create upload directory: " + uploadPath, e);
        }
    }

    private String normalizeUrlPrefix(String value) {
        String prefix = value == null || value.isBlank() ? "/uploads" : value.trim();
        if (!prefix.startsWith("/")) {
            prefix = "/" + prefix;
        }
        if (prefix.endsWith("/")) {
            prefix = prefix.substring(0, prefix.length() - 1);
        }
        return prefix;
    }

    private String normalizeFileLocation(String value) {
        String location = value == null || value.isBlank() ? "uploads" : value.trim();
        location = location.replace("\\", "/");
        if (!location.endsWith("/")) {
            location = location + "/";
        }
        return location;
    }
}
