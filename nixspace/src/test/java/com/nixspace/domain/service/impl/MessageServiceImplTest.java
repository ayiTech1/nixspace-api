package com.nixspace.domain.service.impl;

import com.nixspace.api.dto.request.MessageRequests.*;
import com.nixspace.api.dto.response.Responses.*;
import com.nixspace.api.exception.NixSpaceExceptions.*;
import com.nixspace.api.mapper.MessageMapper;
import com.nixspace.domain.model.*;
import com.nixspace.domain.repository.*;
import com.nixspace.infrastructure.kafka.KafkaEventPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MessageServiceImpl")
class MessageServiceImplTest {

    @Mock MessageRepository         messageRepository;
    @Mock MessageReactionRepository reactionRepository;
    @Mock ChannelMemberRepository   channelMemberRepository;
    @Mock ChannelRepository         channelRepository;
    @Mock UserRepository            userRepository;
    @Mock FileRepository            fileRepository;
    @Mock MessageMapper             messageMapper;
    @Mock KafkaEventPublisher       eventPublisher;

    @InjectMocks MessageServiceImpl messageService;

    // ─── Fixtures ──────────────────────────────────────────────────────────

    private Channel activeChannel(Long id) {
        Workspace ws = new Workspace();
        ws.setId(100L);
        Channel ch = Channel.builder().id(id).workspace(ws).name("general").archived(false).build();
        return ch;
    }

    private MessageResponse mockMessageResponse(Long messageId) {
        User sender = User.builder().id(1L).displayName("Alice").build();
        return new MessageResponse(messageId, 10L,
                new UserSummary(1L, "Alice", null, null),
                null, com.nixspace.common.enums.MessageType.USER,
                "Hello!", com.nixspace.common.enums.ContentType.PLAIN,
                false, false, 0, List.of(), List.of(),
                Instant.now(), Instant.now());
    }

    // ─── sendMessage() ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("sendMessage()")
    class SendMessage {

        @Test
        @DisplayName("persists message and publishes Kafka event")
        void send_success() {
            Long userId = 1L, channelId = 10L;
            var req = new SendMessageRequest("Hello world!", null, null, null);
            Channel channel = activeChannel(channelId);
            Message saved = Message.builder().id(99L).channel(channel).content("Hello world!").build();

            when(channelMemberRepository.existsByChannelIdAndUserId(channelId, userId)).thenReturn(true);
            when(channelRepository.findById(channelId)).thenReturn(Optional.of(channel));
            when(userRepository.getReferenceById(userId)).thenReturn(User.builder().id(userId).build());
            when(messageRepository.save(any(Message.class))).thenReturn(saved);
            when(messageMapper.toResponse(saved)).thenReturn(mockMessageResponse(99L));

            MessageResponse resp = messageService.sendMessage(userId, channelId, req);

            assertThat(resp.id()).isEqualTo(99L);
            verify(messageRepository).save(any(Message.class));
            verify(eventPublisher).publishMessageCreated(any());
        }

        @Test
        @DisplayName("throws AccessDeniedException when user is not a channel member")
        void send_notMember_throws() {
            when(channelMemberRepository.existsByChannelIdAndUserId(10L, 1L)).thenReturn(false);

            assertThatThrownBy(() -> messageService.sendMessage(1L, 10L,
                    new SendMessageRequest("Hi", null, null, null)))
                    .isInstanceOf(AccessDeniedException.class);

            verify(messageRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws BusinessRuleException when channel is archived")
        void send_archivedChannel_throws() {
            Channel ch = activeChannel(10L);
            ch.setArchived(true);

            when(channelMemberRepository.existsByChannelIdAndUserId(10L, 1L)).thenReturn(true);
            when(channelRepository.findById(10L)).thenReturn(Optional.of(ch));

            assertThatThrownBy(() -> messageService.sendMessage(1L, 10L,
                    new SendMessageRequest("Hi", null, null, null)))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("archived");
        }

        @Test
        @DisplayName("increments parent reply count when sending a thread reply")
        void send_threadReply_incrementsReplyCount() {
            Long userId = 1L, channelId = 10L, parentId = 50L;
            var req = new SendMessageRequest("Reply!", null, parentId, null);

            Channel channel = activeChannel(channelId);
            Message parent = Message.builder().id(parentId).channel(channel)
                    .content("Original").deleted(false).build();
            Message saved  = Message.builder().id(100L).channel(channel)
                    .content("Reply!").parentMessage(parent).build();

            when(channelMemberRepository.existsByChannelIdAndUserId(channelId, userId)).thenReturn(true);
            when(channelRepository.findById(channelId)).thenReturn(Optional.of(channel));
            when(userRepository.getReferenceById(userId)).thenReturn(User.builder().id(userId).build());
            when(messageRepository.findByIdAndChannelId(parentId, channelId)).thenReturn(Optional.of(parent));
            when(messageRepository.save(any())).thenReturn(saved);
            when(messageMapper.toResponse(saved)).thenReturn(mockMessageResponse(100L));

            messageService.sendMessage(userId, channelId, req);

            verify(messageRepository).incrementReplyCount(parentId);
        }
    }

    // ─── editMessage() ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("editMessage()")
    class EditMessage {

        @Test
        @DisplayName("updates content and sets edited flag")
        void edit_success() {
            Long userId = 1L, channelId = 10L, msgId = 99L;
            User sender = User.builder().id(userId).build();
            Message msg = Message.builder().id(msgId)
                    .sender(sender).content("Old").deleted(false).build();

            when(messageRepository.findByIdAndChannelId(msgId, channelId)).thenReturn(Optional.of(msg));
            when(messageRepository.save(msg)).thenReturn(msg);
            when(messageMapper.toResponse(msg)).thenReturn(mockMessageResponse(msgId));

            messageService.editMessage(userId, channelId, msgId, new EditMessageRequest("New content"));

            assertThat(msg.getContent()).isEqualTo("New content");
            assertThat(msg.isEdited()).isTrue();
        }

        @Test
        @DisplayName("throws AccessDeniedException when editor is not the sender")
        void edit_notSender_throws() {
            Long senderId = 1L, editorId = 2L, channelId = 10L, msgId = 99L;
            User sender = User.builder().id(senderId).build();
            Message msg = Message.builder().id(msgId).sender(sender).content("Old").deleted(false).build();

            when(messageRepository.findByIdAndChannelId(msgId, channelId)).thenReturn(Optional.of(msg));

            assertThatThrownBy(() -> messageService.editMessage(editorId, channelId, msgId,
                    new EditMessageRequest("Hacked")))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }

    // ─── deleteMessage() ───────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteMessage()")
    class DeleteMessage {

        @Test
        @DisplayName("soft-deletes message and replaces content with [deleted]")
        void delete_byOwner_success() {
            Long userId = 1L, channelId = 10L, msgId = 5L;
            User sender = User.builder().id(userId).build();
            Message msg = Message.builder().id(msgId).sender(sender)
                    .content("Secret").deleted(false).build();

            when(messageRepository.findByIdAndChannelId(msgId, channelId)).thenReturn(Optional.of(msg));
            when(channelMemberRepository.findByChannelIdAndUserId(channelId, userId))
                    .thenReturn(Optional.empty());
            when(messageRepository.save(msg)).thenReturn(msg);
            when(messageMapper.toResponse(msg)).thenReturn(mockMessageResponse(msgId));

            messageService.deleteMessage(userId, channelId, msgId);

            assertThat(msg.isDeleted()).isTrue();
            assertThat(msg.getContent()).isEqualTo("[deleted]");
        }
    }

    // ─── addReaction() ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("addReaction()")
    class AddReaction {

        @Test
        @DisplayName("saves new reaction when not already present")
        void addReaction_success() {
            Long userId = 1L, channelId = 10L, msgId = 99L;
            Message msg = Message.builder().id(msgId).build();

            when(channelMemberRepository.existsByChannelIdAndUserId(channelId, userId)).thenReturn(true);
            when(messageRepository.findByIdAndChannelId(msgId, channelId)).thenReturn(Optional.of(msg));
            when(reactionRepository.existsByMessageIdAndUserIdAndReaction(msgId, userId, ":thumbsup:"))
                    .thenReturn(false);
            when(messageRepository.getReferenceById(msgId)).thenReturn(msg);
            when(userRepository.getReferenceById(userId))
                    .thenReturn(User.builder().id(userId).build());

            messageService.addReaction(userId, channelId, msgId, new AddReactionRequest(":thumbsup:"));

            verify(reactionRepository).save(any(MessageReaction.class));
        }

        @Test
        @DisplayName("throws DuplicateResourceException when reaction already exists")
        void addReaction_duplicate_throws() {
            when(channelMemberRepository.existsByChannelIdAndUserId(10L, 1L)).thenReturn(true);
            when(messageRepository.findByIdAndChannelId(99L, 10L))
                    .thenReturn(Optional.of(Message.builder().id(99L).build()));
            when(reactionRepository.existsByMessageIdAndUserIdAndReaction(99L, 1L, ":thumbsup:"))
                    .thenReturn(true);

            assertThatThrownBy(() -> messageService.addReaction(1L, 10L, 99L,
                    new AddReactionRequest(":thumbsup:")))
                    .isInstanceOf(DuplicateResourceException.class);

            verify(reactionRepository, never()).save(any());
        }
    }
}
