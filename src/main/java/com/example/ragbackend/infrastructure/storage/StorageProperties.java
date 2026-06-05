package com.example.ragbackend.infrastructure.storage;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

    private String localRoot = "storage/documents";
    private List<String> allowedExtensions = List.of("txt", "md", "pdf", "docx");
    private long maxFileSizeBytes = 10 * 1024 * 1024;

    public String getLocalRoot() {
        return localRoot;
    }

    public void setLocalRoot(String localRoot) {
        this.localRoot = localRoot;
    }

    public List<String> getAllowedExtensions() {
        return allowedExtensions;
    }

    public void setAllowedExtensions(List<String> allowedExtensions) {
        this.allowedExtensions = allowedExtensions;
    }

    public long getMaxFileSizeBytes() {
        return maxFileSizeBytes;
    }

    public void setMaxFileSizeBytes(long maxFileSizeBytes) {
        this.maxFileSizeBytes = maxFileSizeBytes;
    }
}
