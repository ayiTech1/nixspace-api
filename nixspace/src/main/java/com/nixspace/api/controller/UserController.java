package com.nixspace.api.controller;

import com.nixspace.api.dto.response.Responses.*;
import com.nixspace.api.mapper.UserMapper;
import com.nixspace.domain.repository.UserRepository;
import com.nixspace.api.exception.NixSpaceExceptions.*;
import com.nixspace.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Users", description = "User profile operations")
public class UserController {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @GetMapping("/me")
    @Operation(summary = "Get the currently authenticated user's profile")
    public UserResponse getMe(@AuthenticationPrincipal UserPrincipal principal) {
        return userRepository.findById(principal.getId())
                .map(userMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("User", principal.getId()));
    }

    @PatchMapping("/me")
    @Operation(summary = "Update display name, status text or timezone")
    public UserResponse updateMe(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody UpdateProfileRequest request
    ) {
        return userRepository.findById(principal.getId())
                .map(user -> {
                    if (request.displayName() != null) user.setDisplayName(request.displayName());
                    if (request.statusText()  != null) user.setStatusText(request.statusText());
                    if (request.timezone()    != null) user.setTimezone(request.timezone());
                    return userMapper.toResponse(userRepository.save(user));
                })
                .orElseThrow(() -> new ResourceNotFoundException("User", principal.getId()));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get a user by ID")
    public UserSummary getUser(@PathVariable Long userId) {
        return userRepository.findById(userId)
                .map(userMapper::toSummary)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    @GetMapping("/search")
    @Operation(summary = "Search users within a workspace")
    public List<UserSummary> search(
            @RequestParam Long workspaceId,
            @RequestParam @Size(min = 1, max = 100) String query
    ) {
        return userRepository.searchInWorkspace(workspaceId, query)
                .stream()
                .map(userMapper::toSummary)
                .toList();
    }

    // ─── Local request record (profile update) ─────────────────────────────

    public record UpdateProfileRequest(
            @Size(min = 2, max = 100) String displayName,
            @Size(max = 100)         String statusText,
                                     String timezone
    ) {}
}
