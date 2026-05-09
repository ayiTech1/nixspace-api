package com.nixspace.domain.model.ids;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceMemberId{
    private String workspaceId;
    private String userId;
}
