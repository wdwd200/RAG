package com.example.ragbackend.infrastructure.storage;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    StoredFile store(MultipartFile file);

    void delete(String storagePath);
}
