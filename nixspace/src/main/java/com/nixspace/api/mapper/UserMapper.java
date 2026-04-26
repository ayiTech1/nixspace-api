package com.nixspace.api.mapper;

import com.nixspace.api.dto.response.Responses.*;
import com.nixspace.domain.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "active", source = "active")
    UserResponse toResponse(User user);

    @Mapping(target = "id",          source = "id")
    @Mapping(target = "displayName", source = "displayName")
    @Mapping(target = "avatarUrl",   source = "avatarUrl")
    @Mapping(target = "statusText",  source = "statusText")
    UserSummary toSummary(User user);
}
