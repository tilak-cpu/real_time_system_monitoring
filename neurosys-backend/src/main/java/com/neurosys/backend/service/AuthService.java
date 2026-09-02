package com.neurosys.backend.service;

import com.neurosys.backend.dto.request.ChangePasswordRequest;
import com.neurosys.backend.dto.request.ForgotPasswordRequest;
import com.neurosys.backend.dto.request.LoginRequest;
import com.neurosys.backend.dto.request.RefreshTokenRequest;
import com.neurosys.backend.dto.request.ResetPasswordRequest;
import com.neurosys.backend.dto.response.AuthResponse;
import com.neurosys.backend.dto.response.TokenRefreshResponse;
import com.neurosys.backend.dto.response.UserProfileResponse;

public interface AuthService {
    AuthResponse login(LoginRequest request);
    TokenRefreshResponse refreshToken(RefreshTokenRequest request);
    void forgotPassword(ForgotPasswordRequest request);
    void resetPassword(ResetPasswordRequest request);
    void changePassword(String userId, ChangePasswordRequest request);
    void logout(String userId);
    UserProfileResponse getCurrentUserProfile(String userId);
}
