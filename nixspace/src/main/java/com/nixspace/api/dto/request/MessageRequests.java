package com.nixspace.api.dto.request;

import com.nixspace.common.enums.ContentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public class MessageRequests {

    public record SendMessageRequest(
            @NotBlank @Size(min = 1, max = 40000, message = "Message content must be 1–40,000 characters")
            String content,

            ContentType contentType,

            Long parentMessageId,   // null = top-level; non-null = thread reply

            List<Long> fileIds      // optional file attachments
    ) {}

    public record EditMessageRequest(
            @NotBlank @Size(min = 1, max = 40000)
            String content
    ) {}

    public record AddReactionRequest(
            @NotBlank @Size(min = 1, max = 50)
            String reaction
    ) {}

    public record MarkReadRequest(
            @NotNull Long messageId
    ) {}
}
