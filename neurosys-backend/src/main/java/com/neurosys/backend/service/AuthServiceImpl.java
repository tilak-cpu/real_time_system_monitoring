package com.neurosys.backend.service;

import com.neurosys.backend.dto.request.ForgotPasswordRequest;
import com.neurosys.backend.dto.request.LoginRequest;
import com.neurosys.backend.dto.request.RefreshTokenRequest;
import com.neurosys.backend.dto.request.ResetPasswordRequest;
import com.neurosys.backend.dto.response.AuthResponse;
import com.neurosys.backend.dto.response.TokenRefreshResponse;
import com.neurosys.backend.dto.response.UserProfileResponse;
import com.neurosys.backend.entity.PasswordResetToken;
import com.neurosys.backend.entity.RefreshToken;
import com.neurosys.backend.entity.User;
import com.neurosys.backend.repository.PasswordResetTokenRepository;
import com.neurosys.backend.repository.RefreshTokenRepository;
import com.neurosys.backend.repository.UserRepository;
import com.neurosys.backend.security.JwtTokenProvider;
import com.neurosys.backend.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final JwtTokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsernameOrEmail(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String accessToken = tokenProvider.generateToken(authentication);
        
        // Create new Refresh Token (7 Days)
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusSeconds(604800))
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }

    @Override
    @Transactional
    public TokenRefreshResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken token = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new RuntimeException("Invalid Refresh Token"));

        if (token.isRevoked() || token.getExpiryDate().isBefore(Instant.now())) {
            throw new RuntimeException("Refresh Token has expired or been revoked");
        }

        User user = token.getUser();
        UserPrincipal userPrincipal = UserPrincipal.create(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities());

        String newAccessToken = tokenProvider.generateToken(authentication);

        return TokenRefreshResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(token.getToken())
                .tokenType("Bearer")
                .build();
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found with email: " + request.getEmail()));

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusSeconds(3600)) // 1 Hour
                .used(false)
                .build();

        passwordResetTokenRepository.save(resetToken);
        log.info("Password reset token generated for user {}: {}", user.getUsername(), resetToken.getToken());
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new RuntimeException("Invalid Password Reset Token"));

        if (resetToken.isUsed() || resetToken.getExpiryDate().isBefore(Instant.now())) {
            throw new RuntimeException("Password Reset Token has expired or already been used");
        }

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
    }

    @Override
    @Transactional
    public void changePassword(String userId, com.neurosys.backend.dto.request.ChangePasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("New password and confirmation do not match.");
        }

        if (request.getNewPassword().length() < 6) {
            throw new IllegalArgumentException("Password does not meet security requirements.");
        }

        User user;
        if (userId != null && !userId.isEmpty()) {
            user = userRepository.findById(userId)
                    .orElseGet(() -> userRepository.findByUsername("admin")
                    .orElseThrow(() -> new RuntimeException("User not found")));
        } else {
            user = userRepository.findByUsername("admin")
                    .orElseThrow(() -> new RuntimeException("User not found"));
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("[INFO] Password changed successfully for user {}", user.getUsername());
    }

    @Override
    @Transactional
    public void logout(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        refreshTokenRepository.deleteByUser(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUserProfile(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .active(user.isActive())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
