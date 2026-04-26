package com.nixspace.domain.service;

import com.nixspace.api.dto.request.AuthRequests.*;
import com.nixspace.api.dto.response.Responses.*;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request, String deviceInfo, String ipAddress);

    AuthResponse refreshToken(String rawRefreshToken);

    void logout(Long userId, String rawRefreshToken);

    void logoutAllDevices(Long userId);

    void changePassword(Long userId, ChangePasswordRequest request);
}
