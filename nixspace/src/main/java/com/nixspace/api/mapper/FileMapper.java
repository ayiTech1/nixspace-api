package com.nixspace.api.mapper;

import com.nixspace.api.dto.response.Responses.*;
import com.nixspace.domain.model.FileEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FileMapper {

    @Mapping(target = "downloadUrl", ignore = true)  // populated by service with presigned URL
    @Mapping(target = "virusScanStatus", source = "virusScanStatus")
    FileResponse toResponse(FileEntity file, String downloadUrl);

    default FileResponse toResponse(FileEntity file) {
        return toResponse(file, null);
    }
}
