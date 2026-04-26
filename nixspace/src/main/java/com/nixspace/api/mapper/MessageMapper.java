package com.nixspace.api.mapper;

import com.nixspace.api.dto.response.Responses.*;
import com.nixspace.domain.model.Message;
import com.nixspace.domain.model.MessageReaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", uses = {UserMapper.class, FileMapper.class})
public interface MessageMapper {

    @Mapping(target = "channelId",       source = "channel.id")
    @Mapping(target = "sender",          source = "sender")
    @Mapping(target = "parentMessageId", source = "parentMessage.id")
    @Mapping(target = "edited",          source = "edited")
    @Mapping(target = "deleted",         source = "deleted")
    @Mapping(target = "reactions",       source = "reactions", qualifiedByName = "aggregateReactions")
    @Mapping(target = "files",           source = "files", qualifiedByName = "mapFiles")
    MessageResponse toResponse(Message message);

    @Named("aggregateReactions")
    default List<ReactionSummary> aggregateReactions(List<MessageReaction> reactions) {
        if (reactions == null || reactions.isEmpty()) return List.of();

        Map<String, List<MessageReaction>> grouped = reactions.stream()
                .collect(Collectors.groupingBy(MessageReaction::getReaction));

        return grouped.entrySet().stream()
                .map(entry -> new ReactionSummary(
                        entry.getKey(),
                        entry.getValue().size(),
                        entry.getValue().stream()
                                .map(r -> r.getUser().getId())
                                .toList()
                ))
                .toList();
    }

    @Named("mapFiles")
    default List<FileResponse> mapFiles(
            List<com.nixspace.domain.model.MessageFile> messageFiles) {
        if (messageFiles == null) return List.of();
        return messageFiles.stream()
                .map(mf -> new FileResponse(
                        mf.getFile().getId(),
                        mf.getFile().getFileName(),
                        mf.getFile().getMimeType(),
                        mf.getFile().getFileSize(),
                        null,  // download URL generated on-demand by FileService
                        mf.getFile().getVirusScanStatus(),
                        mf.getFile().getCreatedAt()
                ))
                .toList();
    }
}
