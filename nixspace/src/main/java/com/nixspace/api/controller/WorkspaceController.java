package com.nixspace.api.controller;

import com.nixspace.api.constants.ResponseCode;
import com.nixspace.api.service.WorkspaceService;
import com.nixspace.domain.request.WorkspaceMemberRequest;
import com.nixspace.domain.request.WorkspaceRequest;
import com.nixspace.domain.response.OperationResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("workspace")
@Slf4j
public class WorkspaceController {

    @Autowired
    WorkspaceService workspaceService;


    @PostMapping("create")
    public OperationResponse createWorkspace(@RequestBody @Validated WorkspaceRequest workspaceRequest) {
        OperationResponse createWorkspaceResponse = workspaceService.createWorkspace(workspaceRequest);
        log.info("Response for createWorkspace {} {}", workspaceRequest, createWorkspaceResponse);
        return createWorkspaceResponse;
    }

    @GetMapping("list/{userId}")
    public OperationResponse listUserWorkspaces(@PathVariable String userId) {
        OperationResponse listUserWorkspacesResponse = workspaceService.listUserWorkspaces(userId);
        log.info("Response for listUserWorkspaces {} {}", userId, listUserWorkspacesResponse);
        return listUserWorkspacesResponse;
    }

    @GetMapping("retrieve/{workspaceId}")
    public OperationResponse getWorkspaceById(@PathVariable String workspaceId) {
        OperationResponse getWorkspaceByIdResponse = workspaceService.getWorkspaceById(workspaceId);
        log.info("Response for getWorkspaceById {} {}", workspaceId, getWorkspaceByIdResponse);
        return getWorkspaceByIdResponse;
    }

    @GetMapping("retrieve-by-slug/{slug}")
    public OperationResponse getWorkspaceBySlug(@PathVariable String slug) {
        OperationResponse getWorkspaceBySlugResponse = workspaceService.getWorkspaceBySlug(slug);
        log.info("Response for getWorkspaceBySlug {} {}", slug, getWorkspaceBySlugResponse);
        return getWorkspaceBySlugResponse;
    }

    @PostMapping("update/{workspaceId}")
    public OperationResponse updateWorkspace(@PathVariable String workspaceId, @RequestBody @Validated WorkspaceRequest workspaceRequest) {
        OperationResponse updateWorkspaceResponse = workspaceService.updateWorkspace(workspaceId, workspaceRequest);
        log.info("Response for updateWorkspace {} {} {}", workspaceId, workspaceRequest, updateWorkspaceResponse);
        return updateWorkspaceResponse;
    }

    @PostMapping("delete/{workspaceId}")
    public OperationResponse deleteWorkspace(@PathVariable String workspaceId) {
        OperationResponse deleteWorkspaceResponse = workspaceService.deleteWorkspace(workspaceId);
        log.info("Response for deleteWorkspace {} {}", workspaceId, deleteWorkspaceResponse);
        return deleteWorkspaceResponse;
    }

    @GetMapping("members/{workspaceId}")
    public OperationResponse listWorkspaceMembers(@PathVariable String workspaceId) {
        OperationResponse listWorkspaceMembersResponse = workspaceService.listWorkspaceMembers(workspaceId);
        log.info("Response for listWorkspaceMembers {} {}", workspaceId, listWorkspaceMembersResponse);
        return listWorkspaceMembersResponse;
    }

    @PostMapping("members/invite/{workspaceId}")
    public OperationResponse inviteMember(@PathVariable String workspaceId, @RequestBody @Validated WorkspaceMemberRequest workspaceMemberRequest) {
        OperationResponse inviteMemberResponse = workspaceService.inviteMember(workspaceId, workspaceMemberRequest);
        log.info("Response for inviteMember {} {} {}", workspaceId, workspaceMemberRequest, inviteMemberResponse);
        return inviteMemberResponse;
    }

    @PostMapping("members/update-role/{workspaceId}/{userId}")
    public OperationResponse updateMemberRole(@PathVariable String workspaceId, @PathVariable String userId,
                                              @RequestBody @Validated WorkspaceMemberRequest workspaceMemberRequest) {
        OperationResponse updateMemberRoleResponse = workspaceService.updateMemberRole(workspaceId, userId, workspaceMemberRequest);
        log.info("Response for updateMemberRole {} {} {} {}", workspaceId, userId, workspaceMemberRequest, updateMemberRoleResponse);
        return updateMemberRoleResponse;
    }

    @PostMapping("members/remove/{workspaceId}/{userId}")
    public OperationResponse removeMember(@PathVariable String workspaceId, @PathVariable String userId) {
        OperationResponse removeMemberResponse = workspaceService.removeMember(workspaceId, userId);
        log.info("Response for removeMember {} {} {}", workspaceId, userId, removeMemberResponse);
        return removeMemberResponse;
    }

    @PostMapping("members/leave/{workspaceId}/{userId}")
    public OperationResponse leaveWorkspace(@PathVariable String workspaceId, @PathVariable String userId) {
        OperationResponse leaveWorkspaceResponse = workspaceService.leaveWorkspace(workspaceId, userId);
        log.info("Response for leaveWorkspace {} {} {}", workspaceId, userId, leaveWorkspaceResponse);
        return leaveWorkspaceResponse;
    }


    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ConstraintViolationException.class)
    public OperationResponse handleConstraintViolationException(ConstraintViolationException e) {
        Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
        StringBuilder strBuilder = new StringBuilder();
        OperationResponse operationResponse = new OperationResponse();
        operationResponse.setResponseCode(ResponseCode.BAD_REQUEST);
        for (ConstraintViolation<?> violation : violations) {
            strBuilder.append(violation.getMessage()).append(", ");
        }
        operationResponse.setResponseMessage(strBuilder.toString());
        return operationResponse;
    }
}