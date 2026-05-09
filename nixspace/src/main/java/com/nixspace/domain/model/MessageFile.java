package com.nixspace.domain.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "message_files")
@Getter @Setter @NoArgsConstructor @Builder @AllArgsConstructor
public class MessageFile {

    @Id
    private String messageFileId;

    private String messageId;

    private String fileEntityId;
}