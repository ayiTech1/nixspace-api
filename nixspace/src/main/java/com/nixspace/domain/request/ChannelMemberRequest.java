package com.nixspace.domain.request;


import com.nixspace.domain.enums.MemberRole;
import lombok.Data;

@Data
public class ChannelMemberRequest {
    private String userId;
    private MemberRole role;
}