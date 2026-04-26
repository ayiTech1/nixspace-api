package com.nixspace.domain.repository;

import com.nixspace.common.enums.ChannelType;
import com.nixspace.domain.model.Channel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChannelRepository extends JpaRepository<Channel, Long> {

    boolean existsByWorkspaceIdAndName(Long workspaceId, String name);

    Optional<Channel> findByIdAndWorkspaceId(Long id, Long workspaceId);

    List<Channel> findAllByWorkspaceIdAndArchivedFalse(Long workspaceId);

    @Query("""
            SELECT c FROM Channel c
            JOIN ChannelMember cm ON cm.channel = c
            WHERE cm.user.id = :userId
            AND c.workspace.id = :workspaceId
            AND c.archived = false
            ORDER BY c.name ASC
            """)
    List<Channel> findAllByWorkspaceIdAndUserId(
            @Param("workspaceId") Long workspaceId,
            @Param("userId") Long userId
    );

    @Query("""
            SELECT c FROM Channel c
            WHERE c.workspace.id = :workspaceId
            AND c.type = :type
            AND c.archived = false
            ORDER BY c.name ASC
            """)
    List<Channel> findByWorkspaceIdAndType(
            @Param("workspaceId") Long workspaceId,
            @Param("type") ChannelType type
    );

    List<Channel> findAllByWorkspaceIdAndDefaultChannelTrue(Long workspaceId);
}
