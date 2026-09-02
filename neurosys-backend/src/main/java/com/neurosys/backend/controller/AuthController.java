package com.neurosys.backend.controller;

import com.neurosys.backend.dto.request.ForgotPasswordRequest;
import com.neurosys.backend.dto.request.LoginRequest;
import com.neurosys.backend.dto.request.RefreshTokenRequest;
import com.neurosys.backend.dto.request.ResetPasswordRequest;
import com.neurosys.backend.dto.response.ApiResponse;
import com.neurosys.backend.dto.response.AuthResponse;
import com.neurosys.backend.dto.response.TokenRefreshResponse;
import com.neurosys.backend.dto.response.UserProfileResponse;
import com.neurosys.backend.security.UserPrincipal;
import com.neurosys.backend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/v1/auth", "/api/auth"})
@RequiredArgsConstructor
@Tag(name = "Authentication Module", description = "Endpoints for Admin/User Authentication, Refresh Token, Profile and Password Management")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "User / Admin Login", description = "Authenticate credentials and return JWT Access Token and Refresh Token")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Authentication successful", response));
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Refresh Access Token", description = "Obtain a new JWT Access Token using a valid Refresh Token")
    public ResponseEntity<ApiResponse<TokenRefreshResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        TokenRefreshResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", response));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Trigger Forgot Password", description = "Generate password reset token for the specified user email")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset instructions generated successfully"));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset Password with Token", description = "Confirm password reset using token and new password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset successfully"));
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change Authenticated User Password", description = "Validate current password and set new password for authenticated user")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody com.neurosys.backend.dto.request.ChangePasswordRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        String userId = userPrincipal != null ? userPrincipal.getId() : null;
        authService.changePassword(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully"));
    }

    @PostMapping("/logout")
    @Operation(summary = "User Logout", description = "Revoke active refresh tokens for the authenticated user")
    public ResponseEntity<ApiResponse<Void>> logout(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        if (userPrincipal != null) {
            authService.logout(userPrincipal.getId());
        }
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
    }

    @GetMapping("/me")
    @Operation(summary = "Get Current User Profile", description = "Retrieve profile details of currently authenticated user")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getCurrentUser(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        UserProfileResponse profile = authService.getCurrentUserProfile(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success("Profile fetched successfully", profile));
    }
}
