//package com.nixspace.api.controller;
//
//import com.nixspace.api.dto.request.AuthRequests.*;
//import com.nixspace.api.dto.response.Responses.*;
//import com.nixspace.domain.service.AuthService;
//import com.nixspace.security.UserPrincipal;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.security.SecurityRequirement;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.validation.Valid;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.core.annotation.AuthenticationPrincipal;
//import org.springframework.web.bind.annotation.*;
//
//@RestController
//@RequestMapping("/auth")
//@RequiredArgsConstructor
//@Tag(name = "Authentication", description = "Register, login, token refresh and logout")
//public class AuthController {
//
//    private final AuthService authService;
//
//    @PostMapping("/register")
//    @ResponseStatus(HttpStatus.CREATED)
//    @Operation(summary = "Register a new user account")
//    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
//        return authService.register(request);
//    }
//
//    @PostMapping("/login")
//    @Operation(summary = "Authenticate and receive access + refresh tokens")
//    public AuthResponse login(
//            @Valid @RequestBody LoginRequest request,
//            HttpServletRequest httpRequest
//    ) {
//        String deviceInfo = httpRequest.getHeader("User-Agent");
//        String ipAddress  = getClientIp(httpRequest);
//        return authService.login(request, deviceInfo, ipAddress);
//    }
//
//    @PostMapping("/refresh")
//    @Operation(summary = "Exchange a valid refresh token for a new token pair")
//    public AuthResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
//        return authService.refreshToken(request.refreshToken());
//    }
//
//    @PostMapping("/logout")
//    @ResponseStatus(HttpStatus.NO_CONTENT)
//    @SecurityRequirement(name = "bearerAuth")
//    @Operation(summary = "Revoke the current refresh token (logout from this device)")
//    public void logout(
//            @AuthenticationPrincipal UserPrincipal principal,
//            @Valid @RequestBody RefreshTokenRequest request
//    ) {
//        authService.logout(principal.getId(), request.refreshToken());
//    }
//
//    @PostMapping("/logout-all")
//    @ResponseStatus(HttpStatus.NO_CONTENT)
//    @SecurityRequirement(name = "bearerAuth")
//    @Operation(summary = "Revoke all refresh tokens (logout from every device)")
//    public void logoutAll(@AuthenticationPrincipal UserPrincipal principal) {
//        authService.logoutAllDevices(principal.getId());
//    }
//
//    @PutMapping("/password")
//    @ResponseStatus(HttpStatus.NO_CONTENT)
//    @SecurityRequirement(name = "bearerAuth")
//    @Operation(summary = "Change the authenticated user's password")
//    public void changePassword(
//            @AuthenticationPrincipal UserPrincipal principal,
//            @Valid @RequestBody ChangePasswordRequest request
//    ) {
//        authService.changePassword(principal.getId(), request);
//    }
//
//    private String getClientIp(HttpServletRequest request) {
//        String xff = request.getHeader("X-Forwarded-For");
//        if (xff != null && !xff.isBlank()) {
//            return xff.split(",")[0].trim();
//        }
//        return request.getRemoteAddr();
//    }
//}
