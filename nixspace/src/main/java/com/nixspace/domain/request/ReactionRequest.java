package com.nixspace.domain.request;

import lombok.Data;

@Data
public class ReactionRequest {
    private String userId;
    private String reaction;
}