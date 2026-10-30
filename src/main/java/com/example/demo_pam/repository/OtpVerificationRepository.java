package com.example.demo_pam.repository;

import com.example.demo_pam.entity.OtpVerification;
import com.example.demo_pam.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface OtpVerificationRepository extends JpaRepository<OtpVerification, UUID> {

    Optional<OtpVerification> findTopByUserAndVerifiedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(User user, Instant now);
}
