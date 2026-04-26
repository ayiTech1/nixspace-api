package com.nixspace.domain.repository;

import com.nixspace.domain.model.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    // Top-level messages in a channel (no parent), ordered newest first
    @Query("""
            SELECT m FROM Message m
            WHERE m.channel.id = :channelId
            AND m.parentMessage IS NULL
            AND m.deleted = false
            ORDER BY m.createdAt DESC
            """)
    Page<Message> findTopLevelByChannelId(
            @Param("channelId") Long channelId,
            Pageable pageable
    );

    // Thread replies for a parent message
    @Query("""
            SELECT m FROM Message m
            WHERE m.parentMessage.id = :parentId
            AND m.deleted = false
            ORDER BY m.createdAt ASC
            """)
    List<Message> findRepliesByParentId(@Param("parentId") Long parentId);

    // Count unread messages in a channel since a specific message ID
    @Query("""
            SELECT COUNT(m) FROM Message m
            WHERE m.channel.id = :channelId
            AND m.id > :lastReadMessageId
            AND m.deleted = false
            AND m.sender.id != :userId
            """)
    long countUnread(
            @Param("channelId") Long channelId,
            @Param("lastReadMessageId") Long lastReadMessageId,
            @Param("userId") Long userId
    );

    Optional<Message> findByIdAndChannelId(Long id, Long channelId);

    @Modifying
    @Query("UPDATE Message m SET m.replyCount = m.replyCount + 1 WHERE m.id = :id")
    void incrementReplyCount(@Param("id") Long id);

    // Fetch messages with sender and reactions eagerly (avoids N+1 in list views)
    @Query("""
            SELECT DISTINCT m FROM Message m
            LEFT JOIN FETCH m.sender
            LEFT JOIN FETCH m.reactions r
            LEFT JOIN FETCH r.user
            WHERE m.channel.id = :channelId
            AND m.parentMessage IS NULL
            AND m.deleted = false
            ORDER BY m.createdAt DESC
            """)
    List<Message> findTopLevelWithDetailsByChannelId(
            @Param("channelId") Long channelId,
            Pageable pageable
    );
}
