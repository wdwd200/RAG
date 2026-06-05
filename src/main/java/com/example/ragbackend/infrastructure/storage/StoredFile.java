package com.example.ragbackend.infrastructure.storage;

public record StoredFile(
        String originalFileName,
        String storagePath,
        long fileSize,
        String fileType
) {
}
