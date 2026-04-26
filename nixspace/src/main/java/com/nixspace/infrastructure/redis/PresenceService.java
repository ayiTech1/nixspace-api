package com.nixspace.infrastructure.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Tracks user presence (online/offline/last seen) using Redis.
 *
 * Key design:
 *   presence:{workspaceId}  -> HASH  { userId -> lastHeartbeatEpochSeconds }
 *   typing:{channelId}      -> SET   { userId } with TTL 5s
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PresenceService {

    private static final String PRESENCE_KEY_PREFIX = "presence:";
    private static final String TYPING_KEY_PREFIX   = "typing:";
    private static final Duration ONLINE_TTL        = Duration.ofMinutes(2);
    private static final Duration TYPING_TTL        = Duration.ofSeconds(5);
    private static final long ONLINE_THRESHOLD_SECS = 120;

    private final RedisTemplate<String, Object> redisTemplate;

    // ─── Heartbeat / Presence ──────────────────────────────────────────────

    public void heartbeat(Long userId, Long workspaceId) {
        String key = PRESENCE_KEY_PREFIX + workspaceId;
        redisTemplate.opsForHash().put(key, String.valueOf(userId), Instant.now().getEpochSecond());
        redisTemplate.expire(key, ONLINE_TTL);
    }

    public void setOffline(Long userId, Long workspaceId) {
        String key = PRESENCE_KEY_PREFIX + workspaceId;
        redisTemplate.opsForHash().delete(key, String.valueOf(userId));
    }

    public boolean isOnline(Long userId, Long workspaceId) {
        String key = PRESENCE_KEY_PREFIX + workspaceId;
        Object value = redisTemplate.opsForHash().get(key, String.valueOf(userId));
        if (value == null) return false;

        long lastSeen = Long.parseLong(value.toString());
        return (Instant.now().getEpochSecond() - lastSeen) < ONLINE_THRESHOLD_SECS;
    }

    public Map<Long, Boolean> getBulkPresence(List<Long> userIds, Long workspaceId) {
        String key = PRESENCE_KEY_PREFIX + workspaceId;
        long now = Instant.now().getEpochSecond();

        return userIds.stream().collect(Collectors.toMap(
                userId -> userId,
                userId -> {
                    Object value = redisTemplate.opsForHash().get(key, String.valueOf(userId));
                    if (value == null) return false;
                    return (now - Long.parseLong(value.toString())) < ONLINE_THRESHOLD_SECS;
                }
        ));
    }

    // ─── Typing Indicators ─────────────────────────────────────────────────

    public void setTyping(Long userId, Long channelId) {
        String key = TYPING_KEY_PREFIX + channelId;
        redisTemplate.opsForSet().add(key, String.valueOf(userId));
        redisTemplate.expire(key, TYPING_TTL);
    }

    public void clearTyping(Long userId, Long channelId) {
        String key = TYPING_KEY_PREFIX + channelId;
        redisTemplate.opsForSet().remove(key, String.valueOf(userId));
    }

    public Set<String> getTypingUsers(Long channelId) {
        String key = TYPING_KEY_PREFIX + channelId;
        Set<Object> members = redisTemplate.opsForSet().members(key);
        if (members == null) return Set.of();
        return members.stream().map(Object::toString).collect(Collectors.toSet());
    }
}
