package com.nixspace.domain.request;


import com.nixspace.domain.enums.UserStatus;
import lombok.Data;

@Data
public class UserRequest {
    private String displayName;
    private String email;
    private UserStatus status;
}