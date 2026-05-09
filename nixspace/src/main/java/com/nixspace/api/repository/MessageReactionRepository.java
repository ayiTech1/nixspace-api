package com.nixspace.api.repository;

import com.nixspace.domain.model.MessageReaction;
import com.nixspace.domain.model.ids.MessageReactionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageReactionRepository extends JpaRepository<MessageReaction, MessageReactionId> {

    boolean existsByMessageIdAndUserIdAndReaction(String messageId, String userId, String reaction);
}