package com.nixspace.api.controller;

import com.nixspace.api.service.PresenceService;
import com.nixspace.domain.request.HeartbeatRequest;
import com.nixspace.domain.request.UserStatusRequest;
import com.nixspace.domain.response.OperationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;



@RestController
@RequestMapping("presence")
@Slf4j
public class PresenceController {

    @Autowired
    PresenceService presenceService;


    @PostMapping("heartbeat")
    public OperationResponse sendHeartbeat(@RequestBody @Validated HeartbeatRequest heartbeatRequest) {
        OperationResponse sendHeartbeatResponse = presenceService.sendHeartbeat(heartbeatRequest);
        log.info("Response for sendHeartbeat {} {}", heartbeatRequest, sendHeartbeatResponse);
        return sendHeartbeatResponse;
    }

    @PostMapping("status")
    public OperationResponse getUserPresenceStatuses(@RequestBody @Validated UserStatusRequest userStatusRequest) {
        OperationResponse getUserPresenceStatusesResponse = presenceService.getUserPresenceStatuses(userStatusRequest);
        log.info("Response for getUserPresenceStatuses {} {}", userStatusRequest, getUserPresenceStatusesResponse);
        return getUserPresenceStatusesResponse;
    }

    @GetMapping("typing/{channelId}")
    public OperationResponse getTypingUsers(@PathVariable String channelId) {
        OperationResponse getTypingUsersResponse = presenceService.getTypingUsers(channelId);
        log.info("Response for getTypingUsers {} {}", channelId, getTypingUsersResponse);
        return getTypingUsersResponse;
    }
}