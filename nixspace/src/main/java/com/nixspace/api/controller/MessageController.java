package com.nixspace.api.controller;

import com.nixspace.api.service.MessageService;
import com.nixspace.domain.request.MessageRequest;
import com.nixspace.domain.request.ReactionRequest;
import com.nixspace.domain.response.OperationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("message")
@Slf4j
public class MessageController {

    @Autowired
    MessageService messageService;


    @PostMapping("send")
    public OperationResponse sendMessage(@RequestBody @Validated MessageRequest messageRequest) {
        OperationResponse sendMessageResponse = messageService.sendMessage(messageRequest);
        log.info("Response for sendMessage {} {}", messageRequest, sendMessageResponse);
        return sendMessageResponse;
    }

    @GetMapping("list/{channelId}")
    public OperationResponse getChannelMessages(@PathVariable String channelId,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size) {
        OperationResponse getChannelMessagesResponse = messageService.getChannelMessages(channelId, page, size);
        log.info("Response for getChannelMessages {} {} {} {}", channelId, page, size, getChannelMessagesResponse);
        return getChannelMessagesResponse;
    }

    @PostMapping("edit/{messageId}/{senderId}")
    public OperationResponse editMessage(@PathVariable String messageId, @PathVariable String senderId,
                                         @RequestBody @Validated MessageRequest messageRequest) {
        OperationResponse editMessageResponse = messageService.editMessage(messageId, senderId, messageRequest);
        log.info("Response for editMessage {} {} {} {}", messageId, senderId, messageRequest, editMessageResponse);
        return editMessageResponse;
    }

    @PostMapping("delete/{messageId}/{senderId}")
    public OperationResponse deleteMessage(@PathVariable String messageId, @PathVariable String senderId) {
        OperationResponse deleteMessageResponse = messageService.deleteMessage(messageId, senderId);
        log.info("Response for deleteMessage {} {} {}", messageId, senderId, deleteMessageResponse);
        return deleteMessageResponse;
    }

    @GetMapping("thread/{parentMessageId}")
    public OperationResponse getThreadReplies(@PathVariable String parentMessageId) {
        OperationResponse getThreadRepliesResponse = messageService.getThreadReplies(parentMessageId);
        log.info("Response for getThreadReplies {} {}", parentMessageId, getThreadRepliesResponse);
        return getThreadRepliesResponse;
    }

    @PostMapping("reaction/add/{messageId}")
    public OperationResponse addReaction(@PathVariable String messageId, @RequestBody @Validated ReactionRequest reactionRequest) {
        OperationResponse addReactionResponse = messageService.addReaction(messageId, reactionRequest);
        log.info("Response for addReaction {} {} {}", messageId, reactionRequest, addReactionResponse);
        return addReactionResponse;
    }

    @PostMapping("reaction/remove/{messageId}/{userId}/{reaction}")
    public OperationResponse removeReaction(@PathVariable String messageId, @PathVariable String userId,
                                            @PathVariable String reaction) {
        OperationResponse removeReactionResponse = messageService.removeReaction(messageId, userId, reaction);
        log.info("Response for removeReaction {} {} {} {}", messageId, userId, reaction, removeReactionResponse);
        return removeReactionResponse;
    }

    @PostMapping("mark-read/{channelId}/{userId}")
    public OperationResponse markChannelAsRead(@PathVariable String channelId, @PathVariable String userId,
                                               @RequestParam String lastReadMessageId) {
        OperationResponse markChannelAsReadResponse = messageService.markChannelAsRead(channelId, userId, lastReadMessageId);
        log.info("Response for markChannelAsRead {} {} {} {}", channelId, userId, lastReadMessageId, markChannelAsReadResponse);
        return markChannelAsReadResponse;
    }


}