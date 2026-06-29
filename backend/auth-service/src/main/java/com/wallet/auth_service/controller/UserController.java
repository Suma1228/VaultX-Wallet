package com.wallet.auth_service.controller;

import com.wallet.auth_service.dto.*;
import com.wallet.auth_service.enums.AccountStatus;
import com.wallet.auth_service.exception.UnauthorizedAccessException;
import com.wallet.auth_service.service.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Management", description = "APIs for user registration, authentication, and profile management")
public class UserController {

    private final UserService userService;

    // ==================== USER REGISTRATION ====================

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponseDTO>> registerUser(
            @Valid @RequestBody UserRegistrationDTO registrationDTO) {

        log.info("POST /api/users/register - Registering user with email: {}", registrationDTO.getEmail());

        UserResponseDTO userResponse = userService.registerUser(registrationDTO);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<UserResponseDTO>builder()
                        .success(true)
                        .message("User registered successfully")
                        .data(userResponse)
                        .timestamp(System.currentTimeMillis())
                        .build()
        );
    }

    // ==================== LOGIN ====================

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDTO>> login(
            @Valid @RequestBody LoginRequestDTO loginRequest) {

        log.info("POST /api/users/login - Login attempt for email: {}", loginRequest.getEmail());

        LoginResponseDTO loginResponse = userService.login(loginRequest);

        return ResponseEntity.ok(
                ApiResponse.<LoginResponseDTO>builder()
                        .success(true)
                        .message("Login successful")
                        .data(loginResponse)
                        .timestamp(System.currentTimeMillis())
                        .build()
        );
    }

    // ==================== LOGOUT ====================

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> logout() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        log.info("POST /api/users/logout - Logout for email: {}", email);

        userService.logout(email);

        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Logout successful")
                        .timestamp(System.currentTimeMillis())
                        .build()
        );
    }

    // ==================== GET PROFILE BY ID ====================

    @GetMapping("/profile/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserResponseDTO>> getUserProfile(
            @PathVariable String userId) {

        log.info("GET /api/users/profile/{} - Fetching profile", userId);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        UserResponseDTO userProfile = userService.getUserProfile(userId);

        // Verify logged-in user matches the requested profile
        if (!userProfile.getEmail().equals(email)) {
            log.warn("User {} attempted to access profile of userId {}", email, userId);
            throw new UnauthorizedAccessException("You can only view your own profile");
        }

        return ResponseEntity.ok(
                ApiResponse.<UserResponseDTO>builder()
                        .success(true)
                        .message("User profile fetched successfully")
                        .data(userProfile)
                        .timestamp(System.currentTimeMillis())
                        .build()
        );
    }

    @GetMapping("/lookup-by-phone")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PhoneLookupResponseDTO>> lookupByPhone(
            @RequestParam String phoneNumber) {

        PhoneLookupResponseDTO result = userService.findUserByPhoneNumber(phoneNumber);

        return ResponseEntity.ok(
                ApiResponse.<PhoneLookupResponseDTO>builder()
                        .success(true)
                        .message("User found")
                        .data(result)
                        .timestamp(System.currentTimeMillis())
                        .build()
        );
    }

    // ==================== GET CURRENT USER (ME) ====================

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserResponseDTO>> getCurrentUserProfile() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // userId is stored as credentials in JwtAuthenticationFilter
        String userId = (String) auth.getCredentials();

        log.info("GET /api/users/me - Fetching profile for userId: {}", userId);

        UserResponseDTO userProfile = userService.getUserProfile(userId);

        return ResponseEntity.ok(
                ApiResponse.<UserResponseDTO>builder()
                        .success(true)
                        .message("User profile fetched successfully")
                        .data(userProfile)
                        .timestamp(System.currentTimeMillis())
                        .build()
        );
    }



    // ==================== UPDATE PROFILE ====================

    @PutMapping("/profile/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserResponseDTO>> updateUserProfile(
            @PathVariable String userId,
            @Valid @RequestBody UserUpdateDTO updateDTO) {

        log.info("PUT /api/users/profile/{} - Updating profile", userId);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        UserResponseDTO existingProfile = userService.getUserProfile(userId);

        if (!existingProfile.getEmail().equals(email)) {
            log.warn("User {} attempted to update profile of userId {}", email, userId);
            throw new UnauthorizedAccessException("You can only update your own profile");
        }

        UserResponseDTO updatedProfile = userService.updateUserProfile(userId, updateDTO);

        return ResponseEntity.ok(
                ApiResponse.<UserResponseDTO>builder()
                        .success(true)
                        .message("User profile updated successfully")
                        .data(updatedProfile)
                        .timestamp(System.currentTimeMillis())
                        .build()
        );
    }

    // ==================== CHANGE PASSWORD ====================

    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordDTO dto) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // userId is stored as credentials in JwtAuthenticationFilter
        String userId = (String) auth.getCredentials();

        log.info("POST /api/users/change-password - Changing password for userId: {}", userId);

        userService.changePassword(userId, dto);

        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Password changed successfully")
                        .timestamp(System.currentTimeMillis())
                        .build()
        );
    }

    // ==================== FORGOT PASSWORD ====================

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordDTO dto) {

        log.info("POST /api/users/forgot-password - Reset request for email: {}", dto.getEmail());

        userService.resetPasswordRequest(dto.getEmail());

        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Password reset link sent to your email")
                        .timestamp(System.currentTimeMillis())
                        .build()
        );
    }

    // ==================== RESET PASSWORD ====================

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordDTO dto) {

        log.info("POST /api/users/reset-password - Resetting password with token");

        userService.resetPassword(dto);

        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Password reset successfully")
                        .timestamp(System.currentTimeMillis())
                        .build()
        );
    }

    // ==================== DEACTIVATE ACCOUNT ====================

    @PatchMapping("/deactivate/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deactivateAccount(
            @PathVariable String userId) {

        log.info("PATCH /api/users/deactivate/{} - Deactivating account", userId);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        UserResponseDTO existingProfile = userService.getUserProfile(userId);

        if (!existingProfile.getEmail().equals(email)) {
            log.warn("User {} attempted to deactivate account of userId {}", email, userId);
            throw new UnauthorizedAccessException("You can only deactivate your own account");
        }

        userService.deactivateAccount(userId);

        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Account deactivated successfully")
                        .timestamp(System.currentTimeMillis())
                        .build()
        );
    }

    // ==================== GET ACCOUNT STATUS ====================

    @GetMapping("/status/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<AccountStatusResponseDTO>> getAccountStatus(
            @PathVariable String userId) {

        log.info("GET /api/users/status/{} - Fetching account status", userId);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        UserResponseDTO existingProfile = userService.getUserProfile(userId);

        if (!existingProfile.getEmail().equals(email)) {
            log.warn("User {} attempted to view status of userId {}", email, userId);
            throw new UnauthorizedAccessException("You can only view your own account status");
        }

        AccountStatus status = userService.getAccountStatus(userId);

        AccountStatusResponseDTO statusResponse = AccountStatusResponseDTO.builder()
                .userId(userId)
                .status(status.toString())
                .build();

        return ResponseEntity.ok(
                ApiResponse.<AccountStatusResponseDTO>builder()
                        .success(true)
                        .message("Account status fetched successfully")
                        .data(statusResponse)
                        .timestamp(System.currentTimeMillis())
                        .build()
        );
    }

    // ==================== HEALTH CHECK ====================

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> healthCheck() {

        return ResponseEntity.ok(
                ApiResponse.<String>builder()
                        .success(true)
                        .message("User service is running")
                        .data("OK")
                        .timestamp(System.currentTimeMillis())
                        .build()
        );
    }
}