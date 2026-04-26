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
import com.nixspace.security.JwtTokenProvider;
import com.nixspace.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl")
class AuthServiceImplTest {

    @Mock UserRepository            userRepository;
    @Mock RefreshTokenRepository    refreshTokenRepository;
    @Mock PasswordEncoder           passwordEncoder;
    @Mock AuthenticationManager     authenticationManager;
    @Mock JwtTokenProvider          jwtTokenProvider;
    @Mock UserMapper                userMapper;
    @Mock NixSpaceProperties        properties;
    @Mock NixSpaceProperties.Jwt    jwtProps;

    @InjectMocks AuthServiceImpl authService;

    // ─── Fixtures ──────────────────────────────────────────────────────────

    private User makeUser(Long id, String email) {
        return User.builder()
                .id(id)
                .email(email)
                .passwordHash("$2a$12$hashed")
                .displayName("Test User")
                .active(true)
                .build();
    }

    private UserResponse makeUserResponse(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName(),
                null, null, "UTC", true, null, Instant.now());
    }

    @BeforeEach
    void setUpProps() {
        when(properties.jwt()).thenReturn(jwtProps);
        when(jwtProps.accessTokenExpiry()).thenReturn(3600000L);
        when(jwtProps.refreshTokenExpiry()).thenReturn(604800000L);
    }

    // ─── register() ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("register()")
    class Register {

        @Test
        @DisplayName("succeeds and returns token pair for a new email")
        void register_success() {
            var req = new RegisterRequest("alice@example.com", "password123", "Alice", "UTC");
            User saved = makeUser(1L, "alice@example.com");

            when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("$2a$12$hashed");
            when(userRepository.save(any(User.class))).thenReturn(saved);
            when(jwtTokenProvider.generateAccessToken(anyLong(), anyString())).thenReturn("access-token");
            when(jwtTokenProvider.generateRefreshToken(anyLong())).thenReturn("refresh-token");
            when(userMapper.toResponse(any())).thenReturn(makeUserResponse(saved));

            AuthResponse resp = authService.register(req);

            assertThat(resp.accessToken()).isEqualTo("access-token");
            assertThat(resp.refreshToken()).isEqualTo("refresh-token");
            assertThat(resp.user().email()).isEqualTo("alice@example.com");
            verify(refreshTokenRepository).save(any(RefreshToken.class));
        }

        @Test
        @DisplayName("throws DuplicateResourceException when email is already in use")
        void register_duplicateEmail_throws() {
            var req = new RegisterRequest("alice@example.com", "password123", "Alice", null);
            when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(req))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("alice@example.com");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("defaults timezone to UTC when not provided")
        void register_defaultsTimezone() {
            var req = new RegisterRequest("bob@example.com", "password123", "Bob", null);
            User saved = makeUser(2L, "bob@example.com");

            when(userRepository.existsByEmail(any())).thenReturn(false);
            when(passwordEncoder.encode(any())).thenReturn("hashed");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                assertThat(u.getTimezone()).isEqualTo("UTC");
                return saved;
            });
            when(jwtTokenProvider.generateAccessToken(anyLong(), anyString())).thenReturn("token");
            when(jwtTokenProvider.generateRefreshToken(anyLong())).thenReturn("rtoken");
            when(userMapper.toResponse(any())).thenReturn(makeUserResponse(saved));

            authService.register(req);
        }
    }

    // ─── login() ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("login()")
    class Login {

        @Test
        @DisplayName("returns token pair on correct credentials")
        void login_success() {
            var req = new LoginRequest("alice@example.com", "password123");
            User user = makeUser(1L, "alice@example.com");
            UserPrincipal principal = UserPrincipal.from(user);

            Authentication auth = mock(Authentication.class);
            when(auth.getPrincipal()).thenReturn(principal);
            when(authenticationManager.authenticate(any())).thenReturn(auth);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(jwtTokenProvider.generateAccessToken(1L, "alice@example.com")).thenReturn("access");
            when(jwtTokenProvider.generateRefreshToken(1L)).thenReturn("refresh");
            when(userMapper.toResponse(user)).thenReturn(makeUserResponse(user));

            AuthResponse resp = authService.login(req, "Mozilla/5.0", "127.0.0.1");

            assertThat(resp.accessToken()).isEqualTo("access");
            verify(userRepository).updateLastSeen(eq(1L), any(Instant.class));
            verify(refreshTokenRepository).save(any(RefreshToken.class));
        }

        @Test
        @DisplayName("throws BadCredentialsException on wrong password")
        void login_wrongPassword_throws() {
            var req = new LoginRequest("alice@example.com", "wrongpassword");
            when(authenticationManager.authenticate(any()))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            assertThatThrownBy(() -> authService.login(req, null, null))
                    .isInstanceOf(BadCredentialsException.class);
        }
    }

    // ─── refreshToken() ────────────────────────────────────────────────────

    @Nested
    @DisplayName("refreshToken()")
    class RefreshToken {

        @Test
        @DisplayName("rotates token pair when refresh token is valid")
        void refresh_success() {
            String rawToken  = "raw-refresh-token";
            User user = makeUser(1L, "alice@example.com");

            com.nixspace.domain.model.RefreshToken stored =
                    com.nixspace.domain.model.RefreshToken.builder()
                            .id(10L)
                            .user(user)
                            .tokenHash("some-hash")
                            .expiresAt(Instant.now().plusSeconds(3600))
                            .revoked(false)
                            .build();

            when(jwtTokenProvider.validateToken(rawToken)).thenReturn(true);
            when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(stored));
            when(jwtTokenProvider.generateAccessToken(anyLong(), anyString())).thenReturn("new-access");
            when(jwtTokenProvider.generateRefreshToken(anyLong())).thenReturn("new-refresh");
            when(userMapper.toResponse(user)).thenReturn(makeUserResponse(user));

            AuthResponse resp = authService.refreshToken(rawToken);

            assertThat(resp.accessToken()).isEqualTo("new-access");
            assertThat(stored.isRevoked()).isTrue();   // old token revoked
            verify(refreshTokenRepository, times(2)).save(any()); // save revoked + save new
        }

        @Test
        @DisplayName("throws InvalidTokenException when JWT is not valid")
        void refresh_invalidJwt_throws() {
            when(jwtTokenProvider.validateToken("bad-token")).thenReturn(false);

            assertThatThrownBy(() -> authService.refreshToken("bad-token"))
                    .isInstanceOf(InvalidTokenException.class);
        }

        @Test
        @DisplayName("revokes all tokens and throws when token has been reused")
        void refresh_revokedToken_revokesAll() {
            String rawToken = "stolen-token";
            User user = makeUser(1L, "alice@example.com");

            com.nixspace.domain.model.RefreshToken revoked =
                    com.nixspace.domain.model.RefreshToken.builder()
                            .id(5L)
                            .user(user)
                            .expiresAt(Instant.now().plusSeconds(3600))
                            .revoked(true)   // already revoked = reuse attack
                            .build();

            when(jwtTokenProvider.validateToken(rawToken)).thenReturn(true);
            when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(revoked));

            assertThatThrownBy(() -> authService.refreshToken(rawToken))
                    .isInstanceOf(InvalidTokenException.class);

            verify(refreshTokenRepository).revokeAllByUserId(1L);
        }
    }

    // ─── logout() ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("logout()")
    class Logout {

        @Test
        @DisplayName("revokes the matching refresh token")
        void logout_revokesToken() {
            User user = makeUser(1L, "alice@example.com");
            com.nixspace.domain.model.RefreshToken token =
                    com.nixspace.domain.model.RefreshToken.builder()
                            .id(1L).user(user).revoked(false)
                            .expiresAt(Instant.now().plusSeconds(600))
                            .build();

            when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

            authService.logout(1L, "raw-token");

            assertThat(token.isRevoked()).isTrue();
            verify(refreshTokenRepository).save(token);
        }
    }

    // ─── changePassword() ──────────────────────────────────────────────────

    @Nested
    @DisplayName("changePassword()")
    class ChangePassword {

        @Test
        @DisplayName("updates hash and revokes all tokens on success")
        void changePassword_success() {
            User user = makeUser(1L, "alice@example.com");
            var req = new ChangePasswordRequest("OldPass1!", "NewPass2!");

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("OldPass1!", user.getPasswordHash())).thenReturn(true);
            when(passwordEncoder.encode("NewPass2!")).thenReturn("new-hash");

            authService.changePassword(1L, req);

            assertThat(user.getPasswordHash()).isEqualTo("new-hash");
            verify(refreshTokenRepository).revokeAllByUserId(1L);
        }

        @Test
        @DisplayName("throws BadRequestException when current password is wrong")
        void changePassword_wrongCurrent_throws() {
            User user = makeUser(1L, "alice@example.com");
            var req = new ChangePasswordRequest("WrongOld!", "NewPass2!");

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("WrongOld!", user.getPasswordHash())).thenReturn(false);

            assertThatThrownBy(() -> authService.changePassword(1L, req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Current password is incorrect");
        }
    }
}
