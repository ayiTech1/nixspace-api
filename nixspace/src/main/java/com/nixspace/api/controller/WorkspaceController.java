package com.nixspace.api.controller;

import com.nixspace.api.dto.request.WorkspaceRequests.*;
import com.nixspace.api.dto.response.Responses.*;
import com.nixspace.domain.service.WorkspaceService;
import com.nixspace.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/workspaces")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Workspaces", description = "Workspace management and membership")
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    // ─── Workspace CRUD ───────────────────────────────────────────────────

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new workspace")
    public WorkspaceResponse createWorkspace(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateWorkspaceRequest request
    ) {
        return workspaceService.createWorkspace(principal.getId(), request);
    }

    @GetMapping
    @Operation(summary = "List all workspaces the authenticated user belongs to")
    public List<WorkspaceResponse> getMyWorkspaces(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return workspaceService.getMyWorkspaces(principal.getId());
    }

    @GetMapping("/{workspaceId}")
    @Operation(summary = "Get a workspace by ID")
    public WorkspaceResponse getWorkspace(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workspaceId
    ) {
        return workspaceService.getWorkspace(principal.getId(), workspaceId);
    }

    @GetMapping("/slug/{slug}")
    @Operation(summary = "Get a workspace by its slug")
    public WorkspaceResponse getBySlug(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String slug
    ) {
        return workspaceService.getWorkspaceBySlug(principal.getId(), slug);
    }

    @PatchMapping("/{workspaceId}")
    @Operation(summary = "Update workspace name or description (Admin/Owner only)")
    public WorkspaceResponse updateWorkspace(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workspaceId,
            @Valid @RequestBody UpdateWorkspaceRequest request
    ) {
        return workspaceService.updateWorkspace(principal.getId(), workspaceId, request);
    }

    @DeleteMapping("/{workspaceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Permanently delete a workspace (Owner only)")
    public void deleteWorkspace(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workspaceId
    ) {
        workspaceService.deleteWorkspace(principal.getId(), workspaceId);
    }

    // ─── Members ──────────────────────────────────────────────────────────

    @GetMapping("/{workspaceId}/members")
    @Operation(summary = "List all members of a workspace")
    public List<WorkspaceMemberResponse> getMembers(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workspaceId
    ) {
        return workspaceService.getMembers(principal.getId(), workspaceId);
    }

    @PostMapping("/{workspaceId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Invite a user to the workspace by email (Admin/Owner only)")
    public WorkspaceMemberResponse inviteMember(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workspaceId,
            @Valid @RequestBody InviteMemberRequest request
    ) {
        return workspaceService.inviteMember(principal.getId(), workspaceId, request);
    }

    @PatchMapping("/{workspaceId}/members/{userId}/role")
    @Operation(summary = "Update a member's role (Owner only)")
    public WorkspaceMemberResponse updateMemberRole(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workspaceId,
            @PathVariable Long userId,
            @Valid @RequestBody UpdateMemberRoleRequest request
    ) {
        return workspaceService.updateMemberRole(principal.getId(), workspaceId, userId, request);
    }

    @DeleteMapping("/{workspaceId}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove a member from the workspace (Admin/Owner only)")
    public void removeMember(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workspaceId,
            @PathVariable Long userId
    ) {
        workspaceService.removeMember(principal.getId(), workspaceId, userId);
    }

    @DeleteMapping("/{workspaceId}/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Leave a workspace")
    public void leaveWorkspace(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workspaceId
    ) {
        workspaceService.leaveWorkspace(principal.getId(), workspaceId);
    }
}
