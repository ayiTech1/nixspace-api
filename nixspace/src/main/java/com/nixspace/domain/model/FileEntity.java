package com.nixspace.domain.model;

import com.nixspace.domain.enums.VirusScanStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
@Entity
@Table(name = "files")
@Getter @Setter @NoArgsConstructor @Builder @AllArgsConstructor
public class FileEntity {

    @Id
    private String fileEntityId;

    private String workspaceId;

    private String uploadedBy;

    private String storageKey;

    private String fileName;

    private String mimeType;

    private String fileSize;

    private String thumbnailKey;

    @Enumerated(EnumType.STRING)
    private VirusScanStatus virusScanStatus;

    private LocalDateTime createdAt;
}