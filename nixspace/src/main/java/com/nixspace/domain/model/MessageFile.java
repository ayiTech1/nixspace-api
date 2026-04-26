package com.nixspace.domain.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "message_files")
@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class MessageFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "file_id", nullable = false)
    private FileEntity file;
}
