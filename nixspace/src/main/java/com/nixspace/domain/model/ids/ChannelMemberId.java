package com.nixspace.domain.model.ids;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChannelMemberId{
    private String channelId;
    private String userId;
}