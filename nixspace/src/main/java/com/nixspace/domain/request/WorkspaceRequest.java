package com.nixspace.domain.request;


import lombok.Data;

@Data
public class WorkspaceRequest {
    private String name;
    private String slug;
    private String description;
    private String iconUrl;
    private String userId;
}