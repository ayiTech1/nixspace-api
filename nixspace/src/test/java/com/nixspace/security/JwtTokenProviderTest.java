package com.nixspace.security;

import com.nixspace.config.NixSpaceProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtTokenProvider")
class JwtTokenProviderTest {

    @Mock NixSpaceProperties          properties;
    @Mock NixSpaceProperties.Jwt      jwtProps;

    JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        when(properties.jwt()).thenReturn(jwtProps);
        when(jwtProps.secret()).thenReturn("NixSpaceSuperSecretKeyForTestingPurposes2024!");
        when(jwtProps.accessTokenExpiry()).thenReturn(3600000L);
        when(jwtProps.refreshTokenExpiry()).thenReturn(604800000L);
        tokenProvider = new JwtTokenProvider(properties);
    }

    @Test
    @DisplayName("generated access token is valid and contains the correct userId")
    void generateAndValidateAccessToken() {
        String token = tokenProvider.generateAccessToken(42L, "alice@example.com");

        assertThat(tokenProvider.validateToken(token)).isTrue();
        assertThat(tokenProvider.getUserIdFromToken(token)).isEqualTo(42L);
        assertThat(tokenProvider.getEmailFromToken(token)).isEqualTo("alice@example.com");
    }

    @Test
    @DisplayName("generated refresh token is valid and contains the correct userId")
    void generateAndValidateRefreshToken() {
        String token = tokenProvider.generateRefreshToken(7L);

        assertThat(tokenProvider.validateToken(token)).isTrue();
        assertThat(tokenProvider.getUserIdFromToken(token)).isEqualTo(7L);
    }

    @Test
    @DisplayName("validateToken returns false for a tampered token")
    void validateToken_tampered_returnsFalse() {
        String token = tokenProvider.generateAccessToken(1L, "test@test.com");
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";

        assertThat(tokenProvider.validateToken(tampered)).isFalse();
    }

    @Test
    @DisplayName("validateToken returns false for an expired token")
    void validateToken_expired_returnsFalse() {
        // Set expiry to -1ms (already expired)
        when(jwtProps.accessTokenExpiry()).thenReturn(-1L);
        JwtTokenProvider expiredProvider = new JwtTokenProvider(properties);

        String token = expiredProvider.generateAccessToken(1L, "test@test.com");
        assertThat(tokenProvider.validateToken(token)).isFalse();
    }

    @Test
    @DisplayName("validateToken returns false for a random string")
    void validateToken_garbage_returnsFalse() {
        assertThat(tokenProvider.validateToken("not.a.jwt")).isFalse();
        assertThat(tokenProvider.validateToken("")).isFalse();
    }
}
