package com.nixspace.domain.repository;

import com.nixspace.domain.model.MessageReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MessageReactionRepository extends JpaRepository<MessageReaction, Long> {

    List<MessageReaction> findAllByMessageId(Long messageId);

    Optional<MessageReaction> findByMessageIdAndUserIdAndReaction(
            Long messageId, Long userId, String reaction
    );

    boolean existsByMessageIdAndUserIdAndReaction(Long messageId, Long userId, String reaction);

    @Modifying
    @Query("""
            DELETE FROM MessageReaction r
            WHERE r.message.id = :messageId
            AND r.user.id = :userId
            AND r.reaction = :reaction
            """)
    void deleteByMessageIdAndUserIdAndReaction(
            @Param("messageId") Long messageId,
            @Param("userId") Long userId,
            @Param("reaction") String reaction
    );

    @Query("""
            SELECT r.reaction, COUNT(r) as count FROM MessageReaction r
            WHERE r.message.id = :messageId
            GROUP BY r.reaction
            """)
    List<Object[]> countGroupedByReaction(@Param("messageId") Long messageId);
}
