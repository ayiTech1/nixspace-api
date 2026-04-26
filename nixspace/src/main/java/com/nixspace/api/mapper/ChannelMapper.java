package com.nixspace.api.mapper;

import com.nixspace.api.dto.response.Responses.*;
import com.nixspace.domain.model.Channel;
import com.nixspace.domain.model.ChannelMember;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface ChannelMapper {

    @Mapping(target = "workspaceId",     source = "workspace.id")
    @Mapping(target = "createdBy",       source = "createdBy")
    @Mapping(target = "archived",        source = "archived")
    @Mapping(target = "defaultChannel",  source = "defaultChannel")
    @Mapping(target = "memberCount",     expression = "java(channel.getMembers().size())")
    ChannelResponse toResponse(Channel channel);

    @Mapping(target = "user",                   source = "user")
    @Mapping(target = "notificationPreference", source = "notificationPreference")
    ChannelMemberResponse toMemberResponse(ChannelMember member);
}
