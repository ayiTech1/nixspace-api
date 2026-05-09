package com.nixspace.api.service;

import com.nixspace.api.constants.ResponseCode;
import com.nixspace.api.constants.ResponseMessage;
import com.nixspace.api.repository.UserRepository;
import com.nixspace.domain.enums.UserStatus;
import com.nixspace.domain.model.User;
import com.nixspace.domain.request.HeartbeatRequest;
import com.nixspace.domain.request.UserStatusRequest;
import com.nixspace.domain.response.OperationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@Slf4j
public class PresenceService {

    private static final String PRESENCE_KEY_PREFIX   = "presence:user:";
    private static final String TYPING_KEY_PREFIX      = "typing:channel:";
    private static final long   HEARTBEAT_TTL_SECONDS  = 60;
    private static final long   TYPING_TTL_SECONDS     = 10;

    @Autowired
    RedisTemplate<String, String> redisTemplate;

    @Autowired
    UserRepository userRepository;


    public OperationResponse sendHeartbeat(HeartbeatRequest heartbeatRequest) {
        log.info("Incoming sendHeartbeat {}", heartbeatRequest);
        try {
            String presenceKey = PRESENCE_KEY_PREFIX.concat(heartbeatRequest.getUserId());

            redisTemplate.opsForValue().set(presenceKey, heartbeatRequest.getWorkspaceId(), Duration.ofSeconds(HEARTBEAT_TTL_SECONDS));

            User user = userRepository.findByUserId(heartbeatRequest.getUserId());
            if (Objects.nonNull(user) && !UserStatus.ONLINE.equals(user.getStatus())) {
                user.setStatus(UserStatus.ONLINE);
                userRepository.save(user);
            }

            return new OperationResponse<>(ResponseCode.CODE_01, ResponseMessage.UPDATE_SUCCESSFUL, null);
        } catch (Exception e) {
            log.error("Error sending heartbeat for user {}: {}", heartbeatRequest.getUserId(), e.getMessage());
            return new OperationResponse<>(ResponseCode.CODE_100, String.format(ResponseMessage.ERROR, "heartbeat", e.getMessage()), null);
        }
    }


    public OperationResponse<Map<String, String>> getUserPresenceStatuses(UserStatusRequest userStatusRequest) {
        log.info("Incoming getUserPresenceStatuses userIds {}", userStatusRequest.getUserIds());
        try {
            Map<String, String> presenceMap = new HashMap<>();

            for (String userId : userStatusRequest.getUserIds()) {
                String presenceKey = PRESENCE_KEY_PREFIX.concat(userId);
                String value = redisTemplate.opsForValue().get(presenceKey);
                presenceMap.put(userId, Objects.nonNull(value) ? UserStatus.ONLINE.name() : UserStatus.OFFLINE.name());
            }

            return new OperationResponse<>(ResponseCode.CODE_01, ResponseMessage.DATA_RETRIEVED, presenceMap);
        } catch (Exception e) {
            log.error("Error retrieving presence statuses: {}", e.getMessage());
            return new OperationResponse<>(ResponseCode.CODE_100, String.format(ResponseMessage.ERROR, "presence", e.getMessage()), null);
        }
    }


    public OperationResponse<Set<String>> getTypingUsers(String channelId) {
        log.info("Incoming getTypingUsers channelId {}", channelId);
        try {
            String typingKey = TYPING_KEY_PREFIX.concat(channelId);
            Set<String> typingUsers = redisTemplate.opsForSet().members(typingKey);

            if (Objects.nonNull(typingUsers) && !typingUsers.isEmpty()) {
                return new OperationResponse<>(ResponseCode.CODE_01, ResponseMessage.DATA_RETRIEVED, typingUsers);
            } else {
                return new OperationResponse<>(ResponseCode.CODE_404, String.format(ResponseMessage.DATA_NOT_FOUND, "typing users"), null);
            }
        } catch (Exception e) {
            log.error("Error retrieving typing users for channel {}: {}", channelId, e.getMessage());
            return new OperationResponse<>(ResponseCode.CODE_100, String.format(ResponseMessage.ERROR, "typing users", e.getMessage()), null);
        }
    }
}