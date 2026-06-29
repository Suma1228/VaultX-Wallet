package com.wallet.auth_service.service;

import com.wallet.auth_service.dto.*;
import com.wallet.auth_service.entity.User;
import com.wallet.auth_service.enums.AccountStatus;
import com.wallet.auth_service.exception.*;
import com.wallet.auth_service.kafka.KafkaProducerService;
import com.wallet.auth_service.repository.UserRepository;
import com.wallet.auth_service.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final KafkaProducerService kafkaProducerService;

    // ===================== USER REGISTRATION =====================
//Kafka is added in following methods : registerUser(), login(), changePassword(), suspendAccount()

    @Override
    public UserResponseDTO registerUser(UserRegistrationDTO registrationDTO) {
        log.info("Registering new user with email: {}", registrationDTO.getEmail());

        // Check if email already exists
        if (userRepository.existsByEmail(registrationDTO.getEmail())) {
            throw new UserAlreadyExistsException("Email already registered: " + registrationDTO.getEmail());
        }

        // Check if phone number already exists
        if (userRepository.existsByPhoneNumber(registrationDTO.getPhoneNumber())) {
            throw new UserAlreadyExistsException("Phone number already registered: " + registrationDTO.getPhoneNumber());
        }

        // Create new user entity
        User user = User.builder()
                .userId(UUID.randomUUID().toString())
                .email(registrationDTO.getEmail())
                .password(passwordEncoder.encode(registrationDTO.getPassword()))
                .firstName(registrationDTO.getFirstName())
                .lastName(registrationDTO.getLastName())
                .phoneNumber(registrationDTO.getPhoneNumber())
                .dateOfBirth(registrationDTO.getDateOfBirth())
                .accountStatus(AccountStatus.ACTIVE)
                .emailVerified(false)
                .phoneVerified(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        User savedUser = userRepository.save(user);
        kafkaProducerService.publishUserEvent(

                UserEvent.builder()
                        .eventType("USER_REGISTERED")
                        .userId(savedUser.getUserId())
                        .email(savedUser.getEmail())
                        .firstName(savedUser.getFirstName())
                        .message("Welcome to WalletX")
                        .timestamp(LocalDateTime.now())
                        .build()
        );
        log.info("User registered successfully with userId: {}", savedUser.getUserId());
        return mapToUserResponseDTO(savedUser);
    }

    // ===================== USER AUTHENTICATION =====================

    @Override
    public LoginResponseDTO login(LoginRequestDTO loginRequest) {
        log.info("Login attempt for user: {}", loginRequest.getEmail());

        // Find user by email
        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        // Check account status
        if (user.getAccountStatus() == AccountStatus.SUSPENDED) {
            throw new AccountSuspendedException("Account is suspended. Please contact support.");
        }

        if (user.getAccountStatus() == AccountStatus.INACTIVE) {
            throw new AccountInactiveException("Account is inactive. Please activate your account.");
        }

        // Authenticate user
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()
                    )
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception e) {
            log.error("Authentication failed for user: {}", loginRequest.getEmail());
            throw new InvalidCredentialsException("Invalid email or password");
        }

        // Update last login time
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        // Generate JWT token
        String token = jwtTokenProvider.generateToken(user);
        log.info("User logged in successfully: {}", user.getUserId());

        kafkaProducerService.publishUserEvent(
                UserEvent.builder()
                        .eventType("USER_LOGGED_IN")
                        .userId(user.getUserId())
                        .email(user.getEmail())
                        .firstName(user.getFirstName())
                        .message("New login detected")
                        .timestamp(LocalDateTime.now())
                        .build()
        );

        return LoginResponseDTO.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getUserId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .accountStatus(user.getAccountStatus().toString())
                .build();
    }

    @Override
    public void logout(String userId) {
        log.info("User logout: {}", userId);

        // Clear security context
        SecurityContextHolder.clearContext();

        // You can add token blacklisting logic here if needed
        // For example: tokenBlacklistService.addToBlacklist(token);

        log.info("User logged out successfully: {}", userId);
    }

    // ===================== USER PROFILE MANAGEMENT =====================

    @Override
    public UserResponseDTO getUserProfile(String userId) {
        log.info("Fetching user profile for userId: {}", userId);

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        return mapToUserResponseDTO(user);
    }

    @Override
    public UserResponseDTO updateUserProfile(String userId, UserUpdateDTO updateDTO) {
        log.info("Updating user profile for userId: {}", userId);

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        // Update fields if provided
        if (updateDTO.getFirstName() != null && !updateDTO.getFirstName().isBlank()) {
            user.setFirstName(updateDTO.getFirstName());
        }

        if (updateDTO.getLastName() != null && !updateDTO.getLastName().isBlank()) {
            user.setLastName(updateDTO.getLastName());
        }

        if (updateDTO.getPhoneNumber() != null && !updateDTO.getPhoneNumber().isBlank()) {
            // Check if new phone number is already taken by another user
            Optional<User> existingUser = userRepository.findByPhoneNumber(updateDTO.getPhoneNumber());
            if (existingUser.isPresent() && !existingUser.get().getUserId().equals(userId)) {
                throw new PhoneNumberAlreadyExistsException("Phone number already in use");
            }
            user.setPhoneNumber(updateDTO.getPhoneNumber());
            user.setPhoneVerified(false); // Reset verification if phone changed
        }

        if (updateDTO.getDateOfBirth() != null) {
            user.setDateOfBirth(updateDTO.getDateOfBirth());
        }

        if (updateDTO.getAddress() != null) {
            user.setAddress(updateDTO.getAddress());
        }

        if (updateDTO.getCity() != null) {
            user.setCity(updateDTO.getCity());
        }

        if (updateDTO.getState() != null) {
            user.setState(updateDTO.getState());
        }

        if (updateDTO.getCountry() != null) {
            user.setCountry(updateDTO.getCountry());
        }

        if (updateDTO.getPinCode() != null) {
            user.setPinCode(updateDTO.getPinCode());
        }

        user.setUpdatedAt(LocalDateTime.now());
        User updatedUser = userRepository.save(user);

        log.info("User profile updated successfully for userId: {}", userId);

        return mapToUserResponseDTO(updatedUser);
    }

    // ===================== PASSWORD MANAGEMENT =====================

    @Override
    public void changePassword(String userId, ChangePasswordDTO changePasswordDTO) {
        log.info("Changing password for userId: {}", userId);

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        // Verify current password
        if (!passwordEncoder.matches(changePasswordDTO.getCurrentPassword(), user.getPassword())) {
            throw new InvalidPasswordException("Current password is incorrect");
        }

        // Validate new password is different from current
        if (changePasswordDTO.getCurrentPassword().equals(changePasswordDTO.getNewPassword())) {
            throw new InvalidPasswordException("New password must be different from current password");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(changePasswordDTO.getNewPassword()));
        user.setPasswordChangedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        kafkaProducerService.publishUserEvent(

                UserEvent.builder()
                        .eventType("PASSWORD_CHANGED")
                        .userId(user.getUserId())
                        .email(user.getEmail())
                        .firstName(user.getFirstName())
                        .message("Your password was changed successfully")
                        .timestamp(LocalDateTime.now())
                        .build()
        );
        log.info("Password changed successfully for userId: {}", userId);
    }

    @Override
    public PhoneLookupResponseDTO findUserByPhoneNumber(String phoneNumber) {
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new UserNotFoundException("No user found with this phone number"));

        return PhoneLookupResponseDTO.builder()
                .userId(user.getUserId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .build();
    }

    @Override
    public void resetPasswordRequest(String email) {
        log.info("Password reset request for email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));

        // Generate reset token
        String resetToken = UUID.randomUUID().toString();
        user.setPasswordResetToken(resetToken);
        user.setPasswordResetTokenExpiry(LocalDateTime.now().plusHours(24)); // Token valid for 24 hours
        userRepository.save(user);

        // Send reset email (integrate with notification service)
        // notificationService.sendPasswordResetEmail(user.getEmail(), resetToken);

        log.info("Password reset token generated for userId: {}", user.getUserId());
    }

    @Override
    public void resetPassword(ResetPasswordDTO resetPasswordDTO) {
        log.info("Resetting password with token");

        User user = userRepository.findByPasswordResetToken(resetPasswordDTO.getToken())
                .orElseThrow(() -> new InvalidTokenException("Invalid or expired reset token"));

        // Check if token has expired
        if (user.getPasswordResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new InvalidTokenException("Reset token has expired");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(resetPasswordDTO.getNewPassword()));
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);
        user.setPasswordChangedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("Password reset successfully for userId: {}", user.getUserId());
    }

    // ===================== ACCOUNT STATUS MANAGEMENT =====================

    @Override
    public void activateAccount(String userId) {
        log.info("Activating account for userId: {}", userId);

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("Account activated successfully for userId: {}", userId);
    }

    @Override
    public void deactivateAccount(String userId) {
        log.info("Deactivating account for userId: {}", userId);

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        user.setAccountStatus(AccountStatus.INACTIVE);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("Account deactivated successfully for userId: {}", userId);
    }

    @Override
    public void suspendAccount(String userId, String reason) {
        log.info("Suspending account for userId: {} with reason: {}", userId, reason);

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        user.setAccountStatus(AccountStatus.SUSPENDED);
        user.setSuspensionReason(reason);
        user.setSuspendedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        kafkaProducerService.publishUserEvent(

                UserEvent.builder()
                        .eventType("ACCOUNT_LOCKED")
                        .userId(user.getUserId())
                        .email(user.getEmail())
                        .firstName(user.getFirstName())
                        .message("Your account has been locked")
                        .timestamp(LocalDateTime.now())
                        .build()
        );

        log.info("Account suspended successfully for userId: {}", userId);
    }

    @Override
    public AccountStatus getAccountStatus(String userId) {
        log.info("Fetching account status for userId: {}", userId);

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        return user.getAccountStatus();
    }

    // ===================== HELPER METHODS =====================

    private UserResponseDTO mapToUserResponseDTO(User user) {
        return UserResponseDTO.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .dateOfBirth(user.getDateOfBirth())
                .address(user.getAddress())
                .city(user.getCity())
                .state(user.getState())
                .country(user.getCountry())
                .pinCode(user.getPinCode())
                .accountStatus(user.getAccountStatus().toString())
                .emailVerified(user.isEmailVerified())
                .phoneVerified(user.isPhoneVerified())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }
}