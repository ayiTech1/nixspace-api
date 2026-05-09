package com.nixspace.domain.model.ids;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageReactionId  {
    private String messageId;
    private String userId;
    private String reaction;
}