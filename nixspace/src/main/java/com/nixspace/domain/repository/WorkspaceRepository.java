package com.nixspace.domain.repository;

import com.nixspace.domain.model.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {

    Optional<Workspace> findBySlug(String slug);

    boolean existsBySlug(String slug);

    @Query("""
            SELECT w FROM Workspace w
            JOIN WorkspaceMember wm ON wm.workspace = w
            WHERE wm.user.id = :userId AND w.active = true
            ORDER BY w.name ASC
            """)
    List<Workspace> findAllByUserId(@Param("userId") Long userId);
}
