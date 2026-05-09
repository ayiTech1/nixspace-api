package com.nixspace.domain.request;

import lombok.Data;

import java.util.List;

@Data
public class UserStatusRequest {
    private List<String> userIds;
}