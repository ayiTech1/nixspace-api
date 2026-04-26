package com.nixspace.domain.service.impl;

import com.nixspace.api.dto.request.AuthRequests.*;
import com.nixspace.api.dto.response.Responses.*;
import com.nixspace.api.exception.NixSpaceExceptions.*;
import com.nixspace.api.mapper.UserMapper;
import com.nixspace.config.NixSpaceProperties;
import com.nixspace.domain.model.RefreshToken;
import com.nixspace.domain.model.User;
import com.nixspace.domain.repository.RefreshTokenRepository;
import com.nixspace.domain.repository.UserRepository;
import com.nixspace.domain.service.AuthService;
import com.nixspace.security.JwtTokenProvider;
import com.nixspace.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserMapper userMapper;
    private final NixSpaceProperties properties;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email already in use: " + request.email());
        }

        User user = User.builder()
                .email(request.email().toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(request.password()))
                .displayName(request.displayName().trim())
                .timezone(request.timezone() != null ? request.timezone() : "UTC")
                .build();

        userRepository.save(user);
        log.info("New user registered: {}", user.getEmail());

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail());
        String rawRefreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        saveRefreshToken(user, rawRefreshToken, null, null);

        return new AuthResponse(
                accessToken,
                rawRefreshToken,
                properties.jwt().accessTokenExpiry() / 1000,
                userMapper.toResponse(user)
        );
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request, String deviceInfo, String ipAddress) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", principal.getId()));

        userRepository.updateLastSeen(user.getId(), Instant.now());

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail());
        String rawRefreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        saveRefreshToken(user, rawRefreshToken, deviceInfo, ipAddress);

        return new AuthResponse(
                accessToken,
                rawRefreshToken,
                properties.jwt().accessTokenExpiry() / 1000,
                userMapper.toResponse(user)
        );
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(String rawRefreshToken) {
        if (!jwtTokenProvider.validateToken(rawRefreshToken)) {
            throw new InvalidTokenException("Invalid or expired refresh token");
        }

        String tokenHash = hashToken(rawRefreshToken);
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidTokenException("Refresh token not found"));

        if (!storedToken.isValid()) {
            // Potential token reuse — revoke all tokens for this user (security measure)
            refreshTokenRepository.revokeAllByUserId(storedToken.getUser().getId());
            throw new InvalidTokenException("Refresh token has been revoked or expired");
        }

        // Rotate: revoke old token, issue new pair
        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        User user = storedToken.getUser();
        String newAccessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail());
        String newRawRefreshToken = jwtTokenProvider.generateRefreshToken(user.getId());
        saveRefreshToken(user, newRawRefreshToken, storedToken.getDeviceInfo(), storedToken.getIpAddress());

        return new AuthResponse(
                newAccessToken,
                newRawRefreshToken,
                properties.jwt().accessTokenExpiry() / 1000,
                userMapper.toResponse(user)
        );
    }

    @Override
    @Transactional
    public void logout(Long userId, String rawRefreshToken) {
        String tokenHash = hashToken(rawRefreshToken);
        refreshTokenRepository.findByTokenHash(tokenHash)
                .filter(t -> t.getUser().getId().equals(userId))
                .ifPresent(t -> {
                    t.setRevoked(true);
                    refreshTokenRepository.save(t);
                });
    }

    @Override
    @Transactional
    public void logoutAllDevices(Long userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        // Revoke all refresh tokens — force re-login on all devices
        refreshTokenRepository.revokeAllByUserId(userId);
    }

    // ─── Private helpers ─────────────────────────────────

    private void saveRefreshToken(User user, String rawToken, String deviceInfo, String ipAddress) {
        long expiryMs = properties.jwt().refreshTokenExpiry();
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(hashToken(rawToken))
                .deviceInfo(deviceInfo)
                .ipAddress(ipAddress)
                .expiresAt(Instant.now().plusMillis(expiryMs))
                .build();
        refreshTokenRepository.save(refreshToken);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
