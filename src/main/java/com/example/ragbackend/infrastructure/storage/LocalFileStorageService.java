package com.example.ragbackend.infrastructure.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import com.example.ragbackend.common.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LocalFileStorageService implements FileStorageService {

    private static final String EMPTY_FILE_CODE = "FILE_EMPTY";
    private static final String EMPTY_FILE_NAME_CODE = "FILE_NAME_EMPTY";
    private static final String INVALID_FILE_NAME_CODE = "INVALID_FILE_NAME";
    private static final String FILE_TYPE_NOT_ALLOWED_CODE = "FILE_TYPE_NOT_ALLOWED";
    private static final String FILE_TOO_LARGE_CODE = "FILE_TOO_LARGE";
    private static final String FILE_STORAGE_FAILED_CODE = "FILE_STORAGE_FAILED";

    private final Path localRoot;
    private final StorageProperties storageProperties;

    public LocalFileStorageService(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
        this.localRoot = Path.of(storageProperties.getLocalRoot()).toAbsolutePath().normalize();
    }

    @Override
    public StoredFile store(MultipartFile file) {
        validateFile(file);
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
        } catch (IOException | SecurityException ex) {
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

        try {
            Path targetFile = localRoot.resolve(storagePath).normalize();
            ensureWithinRoot(targetFile);
            Files.deleteIfExists(targetFile);
        } catch (InvalidPathException ex) {
            throw new BusinessException(INVALID_FILE_NAME_CODE, "Invalid file path");
        } catch (IOException | SecurityException ex) {
            throw new BusinessException(FILE_STORAGE_FAILED_CODE, "Failed to delete stored file");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(EMPTY_FILE_CODE, "Uploaded file must not be empty");
        }
        if (file.getOriginalFilename() == null || file.getOriginalFilename().isBlank()) {
            throw new BusinessException(EMPTY_FILE_NAME_CODE, "Uploaded file name must not be empty");
        }
        if (file.getSize() > storageProperties.getMaxFileSizeBytes()) {
            throw new BusinessException(
                    FILE_TOO_LARGE_CODE,
                    "Uploaded file size exceeds limit: " + storageProperties.getMaxFileSizeBytes() + " bytes"
            );
        }

        String originalFileName = sanitizeOriginalFileName(file.getOriginalFilename());
        String fileType = extractFileType(originalFileName);
        if (!allowedExtensions().contains(fileType)) {
            throw new BusinessException(FILE_TYPE_NOT_ALLOWED_CODE, "File extension is not allowed: " + fileType);
        }
    }

    private String sanitizeOriginalFileName(String originalFilename) {
        String cleanedFileName = StringUtils.cleanPath(originalFilename).replace('\\', '/');

        if (cleanedFileName.contains("..")) {
            throw new BusinessException(INVALID_FILE_NAME_CODE, "Invalid file name");
        }

        int slashIndex = cleanedFileName.lastIndexOf('/');
        String baseName = slashIndex >= 0 ? cleanedFileName.substring(slashIndex + 1) : cleanedFileName;
        if (baseName.isBlank()) {
            throw new BusinessException(EMPTY_FILE_NAME_CODE, "Uploaded file name must not be empty");
        }

        return baseName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String extractFileType(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return "unknown";
        }

        String extension = fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
        return extension.length() > 20 ? extension.substring(0, 20) : extension;
    }

    private Set<String> allowedExtensions() {
        return storageProperties.getAllowedExtensions()
                .stream()
                .map(extension -> extension.toLowerCase(Locale.ROOT).trim())
                .map(extension -> extension.startsWith(".") ? extension.substring(1) : extension)
                .filter(extension -> !extension.isBlank())
                .collect(Collectors.toSet());
    }

    private void ensureWithinRoot(Path targetFile) {
        if (!targetFile.startsWith(localRoot)) {
            throw new BusinessException(INVALID_FILE_NAME_CODE, "Invalid file path");
        }
    }
}
