package com.nixspace.api.mapper;

import com.nixspace.api.dto.response.Responses.*;
import com.nixspace.domain.model.Workspace;
import com.nixspace.domain.model.WorkspaceMember;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface WorkspaceMapper {

    @Mapping(target = "owner",       source = "owner")
    @Mapping(target = "memberCount", expression = "java(workspace.getMembers().size())")
    WorkspaceResponse toResponse(Workspace workspace);

    @Mapping(target = "user", source = "user")
    WorkspaceMemberResponse toMemberResponse(WorkspaceMember member);
}
