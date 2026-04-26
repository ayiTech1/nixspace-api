package com.nixspace.domain.service;

import com.nixspace.api.dto.response.Responses.*;
import org.springframework.data.domain.Pageable;

public interface FileService {
    FileUploadUrlResponse requestUploadUrl(Long userId, Long workspaceId,
                                           String fileName, String mimeType, long fileSize);
    FileResponse confirmUpload(Long userId, Long workspaceId, String storageKey);
    FileResponse getFile(Long userId, Long fileId);
    PagedResponse<FileResponse> getWorkspaceFiles(Long userId, Long workspaceId, Pageable pageable);
    void deleteFile(Long userId, Long fileId);
}
