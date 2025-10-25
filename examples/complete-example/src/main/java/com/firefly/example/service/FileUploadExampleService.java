package com.firefly.example.service;

import com.firefly.common.client.multipart.MultipartUploadHelper;
import com.firefly.common.client.multipart.MultipartConfig;
import com.firefly.common.client.multipart.UploadProgress;
import com.firefly.example.model.UploadResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.Duration;
import java.util.List;

/**
 * Example service demonstrating multipart upload with:
 * - Progress tracking
 * - Chunked uploads for large files
 * - Parallel uploads
 * - File validation
 * - Compression
 */
@Slf4j
@Service
public class FileUploadExampleService {

    private final MultipartUploadHelper uploadClient;

    public FileUploadExampleService() {
        MultipartConfig config = MultipartConfig.builder()
            .timeout(Duration.ofMinutes(5))
            .enableProgressTracking(true)
            .enableCompression(true)
            .enableRetry(true)
            .maxRetries(3)
            .chunkSize(5 * 1024 * 1024) // 5MB chunks
            .maxFileSize(100 * 1024 * 1024) // 100MB max
            .allowedMimeTypes(List.of("image/jpeg", "image/png", "application/pdf"))
            .build();

        this.uploadClient = new MultipartUploadHelper(
            "https://upload.example.com",
            config
        );
        
        log.info("FileUploadExampleService initialized");
    }

    /**
     * Upload a single file with progress tracking.
     */
    public UploadResponse uploadFile(File file) {
        log.info("Uploading file: {}", file.getName());
        
        return uploadClient.uploadFile(
            "/upload",
            file,
            UploadResponse.class,
            this::logProgress
        ).block();
    }

    /**
     * Upload a large file using chunked upload.
     */
    public UploadResponse uploadLargeFile(File file) {
        log.info("Uploading large file in chunks: {}", file.getName());
        
        return uploadClient.uploadFileChunked(
            "/upload/chunked",
            file,
            UploadResponse.class,
            this::logProgress
        ).block();
    }

    /**
     * Upload multiple files in parallel.
     */
    public List<UploadResponse> uploadMultipleFiles(List<File> files) {
        log.info("Uploading {} files in parallel", files.size());
        
        return uploadClient.uploadFilesParallel(
            "/upload",
            files,
            UploadResponse.class,
            this::logProgress
        ).block();
    }

    /**
     * Upload file with metadata.
     */
    public UploadResponse uploadFileWithMetadata(File file, String description, String category) {
        log.info("Uploading file with metadata: {}", file.getName());
        
        return uploadClient.uploadFileWithMetadata(
            "/upload",
            file,
            java.util.Map.of(
                "description", description,
                "category", category
            ),
            UploadResponse.class,
            this::logProgress
        ).block();
    }

    /**
     * Validate file before upload.
     */
    public boolean validateFile(File file) {
        try {
            uploadClient.validateFile(file);
            log.info("File validation passed: {}", file.getName());
            return true;
        } catch (Exception e) {
            log.error("File validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Log upload progress.
     */
    private void logProgress(UploadProgress progress) {
        log.info("Upload progress: {}% ({} / {} bytes) - Speed: {} KB/s - ETA: {}s",
            progress.getPercentage(),
            progress.getBytesUploaded(),
            progress.getTotalBytes(),
            progress.getUploadSpeedKBps(),
            progress.getEstimatedTimeRemainingSeconds()
        );
    }
}

