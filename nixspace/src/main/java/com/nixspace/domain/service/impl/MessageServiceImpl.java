package com.nixspace.domain.service.impl;

import com.nixspace.api.dto.request.MessageRequests.*;
import com.nixspace.api.dto.response.Responses;
import com.nixspace.api.dto.response.Responses.*;
import com.nixspace.api.exception.NixSpaceExceptions.*;
import com.nixspace.api.mapper.MessageMapper;
import com.nixspace.common.enums.ContentType;
import com.nixspace.domain.model.*;
import com.nixspace.domain.repository.*;
import com.nixspace.domain.service.MessageService;
import com.nixspace.infrastructure.kafka.KafkaEventPublisher;
import com.nixspace.infrastructure.kafka.events.MessageEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final MessageReactionRepository reactionRepository;
    private final ChannelMemberRepository channelMemberRepository;
    private final ChannelRepository channelRepository;
    private final UserRepository userRepository;
    private final FileRepository fileRepository;
    private final MessageMapper messageMapper;
    private final KafkaEventPublisher eventPublisher;

    @Override
    @Transactional
    public MessageResponse sendMessage(Long userId, Long channelId, SendMessageRequest request) {
        requireChannelMembership(userId, channelId);

        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new ResourceNotFoundException("Channel", channelId));

        if (channel.isArchived()) {
            throw new BusinessRuleException("Cannot send messages to an archived channel");
        }

        Message message = Message.builder()
                .channel(channel)
                .sender(userRepository.getReferenceById(userId))
                .content(request.content())
                .contentType(request.contentType() != null ? request.contentType() : ContentType.PLAIN)
                .build();

        // Thread reply
        if (request.parentMessageId() != null) {
            Message parent = messageRepository.findByIdAndChannelId(request.parentMessageId(), channelId)
                    .orElseThrow(() -> new ResourceNotFoundException("Parent message not found in this channel"));
            if (parent.isDeleted()) {
                throw new BusinessRuleException("Cannot reply to a deleted message");
            }
            message.setParentMessage(parent);
            messageRepository.incrementReplyCount(parent.getId());
        }

        messageRepository.save(message);

        // Attach files
        if (request.fileIds() != null && !request.fileIds().isEmpty()) {
            request.fileIds().forEach(fileId -> {
                FileEntity file = fileRepository.findById(fileId)
                        .orElseThrow(() -> new ResourceNotFoundException("File", fileId));
                MessageFile mf = MessageFile.builder().message(message).file(file).build();
                message.getFiles().add(mf);
            });
            messageRepository.save(message);
        }

        MessageResponse response = messageMapper.toResponse(message);

        // Publish event to Kafka for real-time fan-out
        eventPublisher.publishMessageCreated(new MessageEvent(
                MessageEvent.Type.CREATED,
                channelId,
                channel.getWorkspace().getId(),
                response
        ));

        log.debug("Message sent: id={} channel={} user={}", message.getId(), channelId, userId);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public Responses.PagedResponse<MessageResponse> getChannelMessages(Long userId, Long channelId, Pageable pageable) {
        requireChannelMembership(userId, channelId);

        Page<Message> page = messageRepository.findTopLevelByChannelId(channelId, pageable);
        List<MessageResponse> content = page.getContent().stream()
                .map(messageMapper::toResponse)
                .toList();

        return new Responses.PagedResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<MessageResponse> getThreadReplies(Long userId, Long channelId, Long parentMessageId) {
        requireChannelMembership(userId, channelId);

        // Verify parent exists in channel
        messageRepository.findByIdAndChannelId(parentMessageId, channelId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found in this channel"));

        return messageRepository.findRepliesByParentId(parentMessageId)
                .stream()
                .map(messageMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public MessageResponse editMessage(Long userId, Long channelId, Long messageId, EditMessageRequest request) {
        Message message = findMessageInChannel(messageId, channelId);
        requireMessageOwnership(userId, message);

        if (message.isDeleted()) {
            throw new BusinessRuleException("Cannot edit a deleted message");
        }

        message.setContent(request.content());
        message.setEdited(true);
        messageRepository.save(message);

        MessageResponse response = messageMapper.toResponse(message);

        eventPublisher.publishMessageCreated(new MessageEvent(
                MessageEvent.Type.UPDATED,
                channelId,
                null,
                response
        ));

        return response;
    }

    @Override
    @Transactional
    public void deleteMessage(Long userId, Long channelId, Long messageId) {
        Message message = findMessageInChannel(messageId, channelId);

        // Owners can delete any message; regular users only their own
        boolean isOwner = checkChannelAdmin(userId, channelId);
        if (!isOwner && !message.getSender().getId().equals(userId)) {
            throw new AccessDeniedException("You can only delete your own messages");
        }

        message.softDelete();
        messageRepository.save(message);

        eventPublisher.publishMessageCreated(new MessageEvent(
                MessageEvent.Type.DELETED,
                channelId,
                null,
                messageMapper.toResponse(message)
        ));
    }

    @Override
    @Transactional
    public void addReaction(Long userId, Long channelId, Long messageId, AddReactionRequest request) {
        requireChannelMembership(userId, channelId);
        findMessageInChannel(messageId, channelId);

        if (reactionRepository.existsByMessageIdAndUserIdAndReaction(messageId, userId, request.reaction())) {
            throw new DuplicateResourceException("Reaction already added");
        }

        MessageReaction reaction = MessageReaction.builder()
                .message(messageRepository.getReferenceById(messageId))
                .user(userRepository.getReferenceById(userId))
                .reaction(request.reaction())
                .build();

        reactionRepository.save(reaction);
    }

    @Override
    @Transactional
    public void removeReaction(Long userId, Long channelId, Long messageId, String reaction) {
        requireChannelMembership(userId, channelId);
        reactionRepository.deleteByMessageIdAndUserIdAndReaction(messageId, userId, reaction);
    }

    @Override
    @Transactional
    public void markRead(Long userId, Long channelId, MarkReadRequest request) {
        requireChannelMembership(userId, channelId);
        channelMemberRepository.updateLastReadMessage(channelId, userId, request.messageId());
    }

    // ─── Private helpers ───────────────────────────────────────────────────

    private void requireChannelMembership(Long userId, Long channelId) {
        if (!channelMemberRepository.existsByChannelIdAndUserId(channelId, userId)) {
            throw new AccessDeniedException("You are not a member of this channel");
        }
    }

    private Message findMessageInChannel(Long messageId, Long channelId) {
        return messageRepository.findByIdAndChannelId(messageId, channelId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found in this channel"));
    }

    private void requireMessageOwnership(Long userId, Message message) {
        if (!message.getSender().getId().equals(userId)) {
            throw new AccessDeniedException("You can only edit your own messages");
        }
    }

    private boolean checkChannelAdmin(Long userId, Long channelId) {
        return channelMemberRepository.findByChannelIdAndUserId(channelId, userId)
                .map(cm -> cm.getRole().name().equals("ADMIN"))
                .orElse(false);
    }
}
