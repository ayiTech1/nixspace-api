package com.nixspace.domain.repository;

import com.nixspace.domain.model.FileEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FileRepository extends JpaRepository<FileEntity, Long> {

    Optional<FileEntity> findByStorageKey(String storageKey);

    Page<FileEntity> findAllByWorkspaceId(Long workspaceId, Pageable pageable);

    Page<FileEntity> findAllByUploadedById(Long userId, Pageable pageable);
}
