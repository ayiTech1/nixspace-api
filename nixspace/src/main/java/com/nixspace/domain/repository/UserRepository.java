package com.nixspace.domain.repository;

import com.nixspace.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.id IN :ids AND u.active = true")
    List<User> findActiveByIds(@Param("ids") List<Long> ids);

    @Modifying
    @Query("UPDATE User u SET u.lastSeenAt = :now WHERE u.id = :userId")
    void updateLastSeen(@Param("userId") Long userId, @Param("now") Instant now);

    @Query("""
            SELECT u FROM User u
            JOIN WorkspaceMember wm ON wm.user = u
            WHERE wm.workspace.id = :workspaceId AND u.active = true
            AND LOWER(u.displayName) LIKE LOWER(CONCAT('%', :query, '%'))
            """)
    List<User> searchInWorkspace(@Param("workspaceId") Long workspaceId,
                                  @Param("query") String query);
}
