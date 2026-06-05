package com.example.ragbackend.infrastructure.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.UUID;
import com.example.ragbackend.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LocalFileStorageService implements FileStorageService {

    private static final String EMPTY_FILE_CODE = "FILE_EMPTY";
    private static final String INVALID_FILE_NAME_CODE = "INVALID_FILE_NAME";
    private static final String FILE_STORAGE_FAILED_CODE = "FILE_STORAGE_FAILED";

    private final Path localRoot;

    public LocalFileStorageService(@Value("${app.storage.local-root:storage/documents}") String localRoot) {
        this.localRoot = Path.of(localRoot).toAbsolutePath().normalize();
    }

    @Override
    public StoredFile store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(EMPTY_FILE_CODE, "Uploaded file must not be empty");
        }

        String originalFileName = sanitizeOriginalFileName(file.getOriginalFilename());
        String fileType = extractFileType(originalFileName);
        String storedFileName = UUID.randomUUID() + "-" + originalFileName;
        String dateDirectory = LocalDate.now().toString();

        Path targetDirectory = localRoot.resolve(dateDirectory).normalize();
        Path targetFile = targetDirectory.resolve(storedFileName).normalize();
        ensureWithinRoot(targetFile);

        try {
            Files.createDirectories(targetDirectory);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetFile);
            }
        } catch (IOException ex) {
            throw new BusinessException(FILE_STORAGE_FAILED_CODE, "Failed to store uploaded file");
        }

        String storagePath = localRoot.relativize(targetFile)
                .toString()
                .replace('\\', '/');
        return new StoredFile(originalFileName, storagePath, file.getSize(), fileType);
    }

    @Override
    public void delete(String storagePath) {
        if (storagePath == null || storagePath.isBlank()) {
            return;
        }

        Path targetFile = localRoot.resolve(storagePath).normalize();
        ensureWithinRoot(targetFile);

        try {
            Files.deleteIfExists(targetFile);
        } catch (IOException ex) {
            throw new BusinessException(FILE_STORAGE_FAILED_CODE, "Failed to delete stored file");
        }
    }

    private String sanitizeOriginalFileName(String originalFilename) {
        String cleanedFileName = StringUtils.cleanPath(
                originalFilename == null || originalFilename.isBlank() ? "file" : originalFilename
        ).replace('\\', '/');

        if (cleanedFileName.contains("..")) {
            throw new BusinessException(INVALID_FILE_NAME_CODE, "Invalid file name");
        }

        int slashIndex = cleanedFileName.lastIndexOf('/');
        String baseName = slashIndex >= 0 ? cleanedFileName.substring(slashIndex + 1) : cleanedFileName;
        if (baseName.isBlank()) {
            baseName = "file";
        }

        return baseName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String extractFileType(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return "unknown";
        }

        String extension = fileName.substring(dotIndex + 1).toLowerCase();
        return extension.length() > 20 ? extension.substring(0, 20) : extension;
    }

    private void ensureWithinRoot(Path targetFile) {
        if (!targetFile.startsWith(localRoot)) {
            throw new BusinessException(INVALID_FILE_NAME_CODE, "Invalid file path");
        }
    }
}
