package com.nixspace.domain.repository;

import com.nixspace.domain.model.ChannelMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChannelMemberRepository extends JpaRepository<ChannelMember, Long> {

    Optional<ChannelMember> findByChannelIdAndUserId(Long channelId, Long userId);

    boolean existsByChannelIdAndUserId(Long channelId, Long userId);

    List<ChannelMember> findAllByChannelId(Long channelId);

    List<ChannelMember> findAllByUserId(Long userId);

    @Modifying
    @Query("""
            UPDATE ChannelMember cm
            SET cm.lastReadMessageId = :messageId
            WHERE cm.channel.id = :channelId AND cm.user.id = :userId
            """)
    void updateLastReadMessage(
            @Param("channelId") Long channelId,
            @Param("userId") Long userId,
            @Param("messageId") Long messageId
    );

    @Modifying
    @Query("DELETE FROM ChannelMember cm WHERE cm.channel.id = :channelId AND cm.user.id = :userId")
    void deleteByChannelIdAndUserId(@Param("channelId") Long channelId, @Param("userId") Long userId);

    long countByChannelId(Long channelId);
}
