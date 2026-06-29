package com.wallet.auth_service.service;


import com.wallet.auth_service.dto.*;
import com.wallet.auth_service.enums.AccountStatus;

public interface UserService {

    // ===================== USER REGISTRATION =====================

    UserResponseDTO registerUser(UserRegistrationDTO registrationDTO);

    // ===================== USER AUTHENTICATION =====================

    LoginResponseDTO login(LoginRequestDTO loginRequest);

    void logout(String userId);

    // ===================== USER PROFILE MANAGEMENT =====================

    UserResponseDTO getUserProfile(String userId);

    UserResponseDTO updateUserProfile(String userId, UserUpdateDTO updateDTO);

    // ===================== PASSWORD MANAGEMENT =====================

    void changePassword(String userId, ChangePasswordDTO changePasswordDTO);

    void resetPasswordRequest(String email);

    void resetPassword(ResetPasswordDTO resetPasswordDTO);

    // ===================== ACCOUNT STATUS MANAGEMENT =====================

    void activateAccount(String userId);

    void deactivateAccount(String userId);

    PhoneLookupResponseDTO findUserByPhoneNumber(String phoneNumber);

    void suspendAccount(String userId, String reason);

    AccountStatus getAccountStatus(String userId);
}