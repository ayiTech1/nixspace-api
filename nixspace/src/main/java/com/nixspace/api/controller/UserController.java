package com.nixspace.api.controller;

import com.nixspace.api.service.UserService;
import com.nixspace.domain.request.UserRequest;
import com.nixspace.domain.response.OperationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;



@RestController
@RequestMapping("user")
@Slf4j
public class UserController {

    @Autowired
    UserService userService;


    @GetMapping("profile/{userId}")
    public OperationResponse getCurrentUserProfile(@PathVariable String userId) {
        OperationResponse getCurrentUserProfileResponse = userService.getCurrentUserProfile(userId);
        log.info("Response for getCurrentUserProfile {} {}", userId, getCurrentUserProfileResponse);
        return getCurrentUserProfileResponse;
    }

    @PostMapping("update/{userId}")
    public OperationResponse updateUser(@PathVariable String userId, @RequestBody @Validated UserRequest userRequest) {
        OperationResponse updateUserResponse = userService.updateUser(userId, userRequest);
        log.info("Response for updateUser {} {} {}", userId, userRequest, updateUserResponse);
        return updateUserResponse;
    }

    @GetMapping("retrieve/{userId}")
    public OperationResponse getUserById(@PathVariable String userId) {
        OperationResponse getUserByIdResponse = userService.getUserById(userId);
        log.info("Response for getUserById {} {}", userId, getUserByIdResponse);
        return getUserByIdResponse;
    }

    @GetMapping("search/{workspaceId}")
    public OperationResponse searchUsersInWorkspace(@PathVariable String workspaceId, @RequestParam String query) {
        OperationResponse searchUsersInWorkspaceResponse = userService.searchUsersInWorkspace(workspaceId, query);
        log.info("Response for searchUsersInWorkspace {} {} {}", workspaceId, query, searchUsersInWorkspaceResponse);
        return searchUsersInWorkspaceResponse;
    }

}