package com.nixspace.domain.service.impl;

import com.nixspace.api.dto.response.Responses.*;
import com.nixspace.api.exception.NixSpaceExceptions.*;
import com.nixspace.api.mapper.FileMapper;
import com.nixspace.domain.model.FileEntity;
import com.nixspace.domain.model.Workspace;
import com.nixspace.domain.model.User;
import com.nixspace.domain.repository.FileRepository;
import com.nixspace.domain.repository.WorkspaceMemberRepository;
import com.nixspace.domain.service.FileService;
import com.nixspace.infrastructure.s3.S3FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final FileRepository fileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final S3FileStorageService s3Service;
    private final FileMapper fileMapper;

    @Override
    @Transactional
    public FileUploadUrlResponse requestUploadUrl(Long userId, Long workspaceId,
                                                   String fileName, String mimeType, long fileSize) {
        requireWorkspaceMembership(userId, workspaceId);

        // Generate presigned URL — file not persisted yet
        S3FileStorageService.PresignedUploadResult result =
                s3Service.generatePresignedUploadUrl(workspaceId, fileName, mimeType, fileSize);

        // Persist a placeholder record so we can attach it to messages before confirm
        Workspace workspace = new Workspace();
        workspace.setId(workspaceId);
        User uploader = new User();
        uploader.setId(userId);

        FileEntity fileEntity = FileEntity.builder()
                .workspace(workspace)
                .uploadedBy(uploader)
                .storageKey(result.storageKey())
                .fileName(fileName)
                .mimeType(mimeType)
                .fileSize(fileSize)
                .build();

        fileRepository.save(fileEntity);
        log.debug("File upload requested: id={} key={}", fileEntity.getId(), result.storageKey());

        return new FileUploadUrlResponse(fileEntity.getId(), result.uploadUrl(), result.storageKey());
    }

    @Override
    @Transactional(readOnly = true)
    public FileResponse confirmUpload(Long userId, Long workspaceId, String storageKey) {
        requireWorkspaceMembership(userId, workspaceId);

        FileEntity file = fileRepository.findByStorageKey(storageKey)
                .orElseThrow(() -> new ResourceNotFoundException("File not found for key: " + storageKey));

        String downloadUrl = s3Service.generatePresignedDownloadUrl(storageKey);
        return fileMapper.toResponse(file, downloadUrl);
    }

    @Override
    @Transactional(readOnly = true)
    public FileResponse getFile(Long userId, Long fileId) {
        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File", fileId));

        requireWorkspaceMembership(userId, file.getWorkspace().getId());

        String downloadUrl = s3Service.generatePresignedDownloadUrl(file.getStorageKey());
        return fileMapper.toResponse(file, downloadUrl);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<FileResponse> getWorkspaceFiles(Long userId, Long workspaceId, Pageable pageable) {
        requireWorkspaceMembership(userId, workspaceId);

        Page<FileEntity> page = fileRepository.findAllByWorkspaceId(workspaceId, pageable);
        List<FileResponse> content = page.getContent().stream()
                .map(f -> fileMapper.toResponse(f, s3Service.generatePresignedDownloadUrl(f.getStorageKey())))
                .toList();

        return new PagedResponse<>(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isLast());
    }

    @Override
    @Transactional
    public void deleteFile(Long userId, Long fileId) {
        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File", fileId));

        if (!file.getUploadedBy().getId().equals(userId)) {
            throw new AccessDeniedException("You can only delete your own files");
        }

        s3Service.deleteFile(file.getStorageKey());
        fileRepository.delete(file);
        log.info("File deleted: id={} key={}", fileId, file.getStorageKey());
    }

    private void requireWorkspaceMembership(Long userId, Long workspaceId) {
        if (!workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)) {
            throw new AccessDeniedException("You are not a member of this workspace");
        }
    }
}
