//package com.nixspace.api.service;
//
//import com.nixspace.domain.enums.VirusScanStatus;
//import com.nixspace.domain.model.FileEntity;
//import com.nixspace.domain.response.OperationResponse;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.domain.Sort;
//import org.springframework.stereotype.Service;
//
//import java.util.Objects;
//import java.util.UUID;
//
//@Service
//@Slf4j
//public class FileService {
//
//    @Autowired
//    FileEntityRepository fileEntityRepository;
//
//    @Autowired
//    StorageService storageService;
//
//
//    public OperationResponse<FileEntity> uploadFileRecord(FileUploadRequest fileUploadRequest) {
//        log.info("Incoming uploadFileRecord {}", fileUploadRequest);
//        try {
//            FileEntity fileEntity = FileEntity.builder()
//                    .fileEntityId(UUID.randomUUID().toString())
//                    .workspaceId(fileUploadRequest.getWorkspaceId())
//                    .uploadedBy(fileUploadRequest.getUploadedBy())
//                    .storageKey(fileUploadRequest.getStorageKey())
//                    .fileName(fileUploadRequest.getFileName())
//                    .mimeType(fileUploadRequest.getMimeType())
//                    .fileSize(fileUploadRequest.getFileSize())
//                    .thumbnailKey(fileUploadRequest.getThumbnailKey())
//                    .virusScanStatus(VirusScanStatus.PENDING)
//                    .createdAt(java.time.LocalDateTime.now())
//                    .build();
//
//            FileEntity savedFile = fileEntityRepository.save(fileEntity);
//
//            if (Objects.nonNull(savedFile)) {
//                return new OperationResponse<>(ResponseCode.CODE_01, ResponseMessage.DATA_ADDED, savedFile);
//            } else {
//                return new OperationResponse<>(ResponseCode.CODE_100, ResponseMessage.FAILED_TO_ADD, null);
//            }
//        } catch (Exception e) {
//            log.error("Error uploading file record: {}", e.getMessage());
//            return new OperationResponse<>(ResponseCode.CODE_100, String.format(ResponseMessage.ERROR, "file", e.getMessage()), null);
//        }
//    }
//
//
//    public OperationResponse<Page<FileEntity>> listWorkspaceFiles(String workspaceId, int page, int size) {
//        log.info("Incoming listWorkspaceFiles workspaceId {} page {} size {}", workspaceId, page, size);
//        try {
//            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
//            Page<FileEntity> files = fileEntityRepository.findByWorkspaceId(workspaceId, pageable);
//
//            if (Objects.nonNull(files) && !files.isEmpty()) {
//                return new OperationResponse<>(ResponseCode.CODE_01, ResponseMessage.DATA_RETRIEVED, files);
//            } else {
//                return new OperationResponse<>(ResponseCode.CODE_404, String.format(ResponseMessage.DATA_NOT_FOUND, "files"), null);
//            }
//        } catch (Exception e) {
//            log.error("Error listing files for workspace {}: {}", workspaceId, e.getMessage());
//            return new OperationResponse<>(ResponseCode.CODE_100, String.format(ResponseMessage.ERROR, "files", e.getMessage()), null);
//        }
//    }
//
//
//    public OperationResponse<FileResponse> getFileRecord(String fileEntityId) {
//        log.info("Incoming getFileRecord fileEntityId {}", fileEntityId);
//        try {
//            FileEntity fileEntity = fileEntityRepository.findByFileEntityId(fileEntityId);
//
//            if (Objects.isNull(fileEntity)) {
//                return new OperationResponse<>(ResponseCode.CODE_404, String.format(ResponseMessage.DATA_NOT_FOUND, "file"), null);
//            }
//
//            String presignedUrl = storageService.generatePresignedDownloadUrl(fileEntity.getStorageKey());
//
//            FileResponse fileResponse = FileResponse.builder()
//                    .fileEntity(fileEntity)
//                    .presignedDownloadUrl(presignedUrl)
//                    .build();
//
//            return new OperationResponse<>(ResponseCode.CODE_01, ResponseMessage.DATA_RETRIEVED, fileResponse);
//        } catch (Exception e) {
//            log.error("Error retrieving file record {}: {}", fileEntityId, e.getMessage());
//            return new OperationResponse<>(ResponseCode.CODE_100, String.format(ResponseMessage.ERROR, "file", e.getMessage()), null);
//        }
//    }
//
//
//    public OperationResponse deleteFile(String fileEntityId, String uploadedBy) {
//        log.info("Incoming deleteFile fileEntityId {} uploadedBy {}", fileEntityId, uploadedBy);
//        try {
//            FileEntity fileEntity = fileEntityRepository.findByFileEntityId(fileEntityId);
//
//            if (Objects.isNull(fileEntity)) {
//                return new OperationResponse<>(ResponseCode.CODE_404, String.format(ResponseMessage.DATA_NOT_FOUND, "file"), null);
//            }
//
//            if (!fileEntity.getUploadedBy().equals(uploadedBy)) {
//                return new OperationResponse<>(ResponseCode.CODE_100, ResponseMessage.ACCESS_DENIED, null);
//            }
//
//            storageService.deleteFile(fileEntity.getStorageKey());
//            fileEntityRepository.delete(fileEntity);
//
//            return new OperationResponse<>(ResponseCode.CODE_01, String.format(ResponseMessage.DATA_CLEARED, "file", fileEntityId), null);
//        } catch (Exception e) {
//            log.error("Error deleting file {} for uploader {}: {}", fileEntityId, uploadedBy, e.getMessage());
//            return new OperationResponse<>(ResponseCode.CODE_100, String.format(ResponseMessage.ERROR, "file", e.getMessage()), null);
//        }
//    }
//}
