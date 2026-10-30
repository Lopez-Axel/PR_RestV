package com.example.demo_pam.service;

import com.example.demo_pam.dto.request.LoginRequest;
import com.example.demo_pam.dto.request.RefreshRequest;
import com.example.demo_pam.dto.request.RegisterRequest;
import com.example.demo_pam.dto.request.VerifyOtpRequest;
import com.example.demo_pam.dto.response.AuthResponse;
import com.example.demo_pam.dto.response.UserResponse;
import com.example.demo_pam.entity.OtpVerification;
import com.example.demo_pam.entity.RefreshToken;
import com.example.demo_pam.entity.User;
import com.example.demo_pam.exception.ResourceNotFoundException;
import com.example.demo_pam.repository.OtpVerificationRepository;
import com.example.demo_pam.repository.RefreshTokenRepository;
import com.example.demo_pam.repository.UserRepository;
import com.example.demo_pam.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OtpVerificationRepository otpVerificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final long refreshExpirationMs;
    private final long otpExpirationMinutes;

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       OtpVerificationRepository otpVerificationRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       ObjectProvider<JavaMailSender> mailSenderProvider,
                       @Value("${app.jwt.refresh-expiration-ms}") long refreshExpirationMs,
                       @Value("${app.jwt.otp-expiration-minutes}") long otpExpirationMinutes) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.otpVerificationRepository = otpVerificationRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.mailSenderProvider = mailSenderProvider;
        this.refreshExpirationMs = refreshExpirationMs;
        this.otpExpirationMinutes = otpExpirationMinutes;
    }

    // Asegura atomicidad: si falla el insert de OtpVerification se revierte el User ya creado
    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("El email ya está registrado");
        }
        User user = new User();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        userRepository.save(user);

        String code = generateOtpCode();
        OtpVerification otp = new OtpVerification();
        otp.setUser(user);
        otp.setOtpHash(passwordEncoder.encode(code));
        otp.setExpiresAt(Instant.now().plus(otpExpirationMinutes, ChronoUnit.MINUTES));
        otpVerificationRepository.save(otp);

        deliverOtp(user.getEmail(), code);
    }

    // Escribe en dos tablas (otp_verifications y users): si falla una se revierte la otra
    @Transactional
    public void verifyOtp(VerifyOtpRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + request.email()));
        OtpVerification otp = otpVerificationRepository
                .findTopByUserAndVerifiedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(user, Instant.now())
                .orElseThrow(() -> new IllegalArgumentException("No hay un código de verificación vigente"));
        if (!passwordEncoder.matches(request.code(), otp.getOtpHash())) {
            throw new IllegalArgumentException("Código incorrecto");
        }
        otp.setVerifiedAt(Instant.now());
        user.setStatus(User.UserStatus.ACTIVE);
        user.setEmailVerifiedAt(Instant.now());
        userRepository.save(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Credenciales inválidas");
        }
        if (user.getStatus() != User.UserStatus.ACTIVE) {
            throw new IllegalArgumentException("La cuenta aún no está verificada");
        }
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = issueRefreshToken(user);
        return buildAuthResponse(accessToken, refreshToken, user);
    }

    // Escribe en dos tablas (revoca el token anterior y crea el nuevo): debe ser atómico
    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        RefreshToken stored = refreshTokenRepository.findByTokenHash(sha256Hex(request.refreshToken()))
                .orElseThrow(() -> new BadCredentialsException("Refresh token inválido"));
        if (stored.getRevokedAt() != null || stored.getExpiresAt().isBefore(Instant.now())) {
            throw new BadCredentialsException("Refresh token expirado o revocado");
        }
        stored.setRevokedAt(Instant.now());

        String accessToken = jwtService.generateAccessToken(stored.getUser());
        String refreshToken = issueRefreshToken(stored.getUser());
        return buildAuthResponse(accessToken, refreshToken, stored.getUser());
    }

    private String issueRefreshToken(User user) {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        RefreshToken entity = new RefreshToken();
        entity.setUser(user);
        entity.setTokenHash(sha256Hex(raw));
        entity.setExpiresAt(Instant.now().plusMillis(refreshExpirationMs));
        refreshTokenRepository.save(entity);
        return raw;
    }

    private AuthResponse buildAuthResponse(String accessToken, String refreshToken, User user) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", jwtService.getAccessExpirationSeconds(),
                UserResponse.from(user));
    }

    private void deliverOtp(String email, String code) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.info("[OTP] SMTP no configurado; código de verificación para {}: {}", email, code);
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Tu código de verificación");
        message.setText("Tu código de verificación es: " + code
                + "\nVence en " + otpExpirationMinutes + " minutos.");
        try {
            mailSender.send(message);
        } catch (Exception e) {
            log.error("[OTP] Falló el envío del email a {}: {}", email, e.getMessage());
            throw new IllegalStateException("No se pudo enviar el código de verificación", e);
        }
    }

    private String generateOtpCode() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
