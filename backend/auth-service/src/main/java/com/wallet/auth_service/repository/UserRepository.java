package com.wallet.auth_service.repository;

import com.wallet.auth_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // ===================== FINDERS =====================

    Optional<User> findByEmail(String email);

    Optional<User> findByUserId(String userId);

    Optional<User> findByPhoneNumber(String phoneNumber);

    Optional<User> findByPasswordResetToken(String token);

    // ===================== EXISTENCE CHECKS =====================

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);
}