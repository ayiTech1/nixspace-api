package com.nixspace.api.service;


import com.nixspace.api.constants.ResponseCode;
import com.nixspace.api.constants.ResponseMessage;
import com.nixspace.api.repository.UserRepository;
import com.nixspace.api.repository.WorkspaceMemberRepository;
import com.nixspace.domain.model.User;
import com.nixspace.domain.request.UserRequest;
import com.nixspace.domain.response.OperationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class UserService {

    @Autowired
    UserRepository userRepository;

    @Autowired
    WorkspaceMemberRepository workspaceMemberRepository;


    public OperationResponse<User> getCurrentUserProfile(String userId) {
        log.info("Incoming getCurrentUserProfile userId {}", userId);
        try {
            User user = userRepository.findByUserId(userId);

            if (Objects.nonNull(user)) {
                return new OperationResponse<>(ResponseCode.CODE_01, ResponseMessage.DATA_RETRIEVED, user);
            } else {
                return new OperationResponse<>(ResponseCode.CODE_404, String.format(ResponseMessage.DATA_NOT_FOUND, "user"), null);
            }
        } catch (Exception e) {
            log.error("Error retrieving profile for user {}: {}", userId, e.getMessage());
            return new OperationResponse<>(ResponseCode.CODE_100, String.format(ResponseMessage.ERROR, "user", e.getMessage()), null);
        }
    }


    public OperationResponse<User> updateUser(String userId, UserRequest userRequest) {
        log.info("Incoming updateUser userId {} request {}", userId, userRequest);
        try {
            User user = userRepository.findByUserId(userId);

            if (Objects.nonNull(user)) {
                user.setDisplayName(userRequest.getDisplayName());
                user.setEmail(userRequest.getEmail());
                user.setStatus(userRequest.getStatus());

                User updatedUser = userRepository.save(user);
                return new OperationResponse<>(ResponseCode.CODE_01, ResponseMessage.UPDATE_SUCCESSFUL, updatedUser);
            } else {
                return new OperationResponse<>(ResponseCode.CODE_404, String.format(ResponseMessage.DATA_NOT_FOUND, "user"), null);
            }
        } catch (Exception e) {
            log.error("Error updating user {}: {}", userId, e.getMessage());
            return new OperationResponse<>(ResponseCode.CODE_100, String.format(ResponseMessage.ERROR, "user", e.getMessage()), null);
        }
    }


    public OperationResponse<User> getUserById(String userId) {
        log.info("Incoming getUserById userId {}", userId);
        try {
            User user = userRepository.findByUserId(userId);

            if (Objects.nonNull(user)) {
                return new OperationResponse<>(ResponseCode.CODE_01, ResponseMessage.DATA_RETRIEVED, user);
            } else {
                return new OperationResponse<>(ResponseCode.CODE_404, String.format(ResponseMessage.DATA_NOT_FOUND, "user"), null);
            }
        } catch (Exception e) {
            log.error("Error retrieving user {}: {}", userId, e.getMessage());
            return new OperationResponse<>(ResponseCode.CODE_100, String.format(ResponseMessage.ERROR, "user", e.getMessage()), null);
        }
    }


    public OperationResponse<List<User>> searchUsersInWorkspace(String workspaceId, String query) {
        log.info("Incoming searchUsersInWorkspace workspaceId {} query {}", workspaceId, query);
        try {
            List<String> userIds = workspaceMemberRepository.findByWorkspaceId(workspaceId)
                    .stream()
                    .map(member -> member.getUserId())
                    .toList();

            if (userIds.isEmpty()) {
                return new OperationResponse<>(ResponseCode.CODE_404, String.format(ResponseMessage.DATA_NOT_FOUND, "users"), null);
            }

            List<User> users = userRepository.findByUserIdInAndDisplayNameContainingIgnoreCase(userIds, query);

            if (Objects.nonNull(users) && !users.isEmpty()) {
                return new OperationResponse<>(ResponseCode.CODE_01, ResponseMessage.DATA_RETRIEVED, users);
            } else {
                return new OperationResponse<>(ResponseCode.CODE_404, String.format(ResponseMessage.DATA_NOT_FOUND, "users"), null);
            }
        } catch (Exception e) {
            log.error("Error searching users in workspace {} with query {}: {}", workspaceId, query, e.getMessage());
            return new OperationResponse<>(ResponseCode.CODE_100, String.format(ResponseMessage.ERROR, "users", e.getMessage()), null);
        }
    }
}