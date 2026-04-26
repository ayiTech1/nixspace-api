package com.nixspace.infrastructure.s3;

import com.nixspace.api.dto.response.Responses.FileUploadUrlResponse;
import com.nixspace.api.exception.NixSpaceExceptions.BadRequestException;
import com.nixspace.config.NixSpaceProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3FileStorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final NixSpaceProperties properties;

    private static final List<String> ALLOWED_MIME_TYPES = List.of(
            "image/jpeg", "image/png", "image/gif", "image/webp",
            "application/pdf", "text/plain",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    /**
     * Generates a presigned PUT URL for direct client-to-S3 upload.
     * The client uploads the file, then notifies the API to create the file record.
     */
    public PresignedUploadResult generatePresignedUploadUrl(
            Long workspaceId, String fileName, String mimeType, long fileSize) {

        validateMimeType(mimeType);
        validateFileSize(fileSize);

        String storageKey = buildStorageKey(workspaceId, fileName);
        String bucket = properties.aws().s3().bucket();
        long expirySeconds = properties.aws().s3().presignedUrlExpiry();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(expirySeconds))
                .putObjectRequest(req -> req
                        .bucket(bucket)
                        .key(storageKey)
                        .contentType(mimeType)
                        .contentLength(fileSize)
                )
                .build();

        String uploadUrl = s3Presigner.presignPutObject(presignRequest).url().toString();
        log.debug("Generated presigned upload URL for key={}", storageKey);
        return new PresignedUploadResult(storageKey, uploadUrl);
    }

    /**
     * Generates a presigned GET URL for secure file download.
     */
    public String generatePresignedDownloadUrl(String storageKey) {
        String bucket = properties.aws().s3().bucket();
        long expirySeconds = properties.aws().s3().presignedUrlExpiry();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(expirySeconds))
                .getObjectRequest(req -> req.bucket(bucket).key(storageKey))
                .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    /**
     * Deletes a file from S3 storage.
     */
    public void deleteFile(String storageKey) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(properties.aws().s3().bucket())
                    .key(storageKey)
                    .build());
            log.info("Deleted S3 object: {}", storageKey);
        } catch (Exception ex) {
            log.error("Failed to delete S3 object [key={}]: {}", storageKey, ex.getMessage());
        }
    }

    // ─── Private helpers ───────────────────────────────────────────────────

    private String buildStorageKey(Long workspaceId, String fileName) {
        String ext = extractExtension(fileName);
        return String.format("workspaces/%d/files/%s%s", workspaceId, UUID.randomUUID(), ext);
    }

    private String extractExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex > 0) ? fileName.substring(dotIndex) : "";
    }

    private void validateMimeType(String mimeType) {
        if (!ALLOWED_MIME_TYPES.contains(mimeType)) {
            throw new BadRequestException("File type not allowed: " + mimeType);
        }
    }

    private void validateFileSize(long fileSize) {
        long maxSize = properties.upload().maxFileSize();
        if (fileSize > maxSize) {
            throw new BadRequestException(
                    "File size (%d bytes) exceeds maximum allowed (%d bytes)".formatted(fileSize, maxSize)
            );
        }
    }

    public record PresignedUploadResult(String storageKey, String uploadUrl) {}
}
