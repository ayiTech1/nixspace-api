package com.nixspace.api.repository;

import com.nixspace.domain.model.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, String> {

    Message findByMessageId(String messageId);

    Page<Message> findByChannelIdAndParentMessageIdIsNullAndDeletedFalse(String channelId, Pageable pageable);

    List<Message> findByParentMessageIdAndDeletedFalseOrderByCreatedAtAsc(String parentMessageId);
}