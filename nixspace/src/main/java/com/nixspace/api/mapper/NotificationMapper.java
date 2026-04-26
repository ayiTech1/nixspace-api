package com.nixspace.api.mapper;

import com.nixspace.api.dto.response.Responses.*;
import com.nixspace.domain.model.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface NotificationMapper {

    @Mapping(target = "actor", source = "actor")
    @Mapping(target = "read",  source = "read")
    NotificationResponse toResponse(Notification notification);
}
