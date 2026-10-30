package com.example.demo_pam.controller;

import com.example.demo_pam.dto.request.LoginRequest;
import com.example.demo_pam.dto.request.RefreshRequest;
import com.example.demo_pam.dto.request.RegisterRequest;
import com.example.demo_pam.dto.request.VerifyOtpRequest;
import com.example.demo_pam.dto.response.AuthResponse;
import com.example.demo_pam.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(
            // Dispara la validación de las restricciones del DTO antes de ejecutar el método
            @Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Registro iniciado; revisa tu email para el código de verificación"));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, String>> verifyOtp(
            // Dispara la validación de las restricciones del DTO antes de ejecutar el método
            @Valid @RequestBody VerifyOtpRequest request) {
        authService.verifyOtp(request);
        return ResponseEntity.ok(Map.of("message", "Cuenta verificada correctamente"));
    }

    @PostMapping("/login")
    public AuthResponse login(
            // Dispara la validación de las restricciones del DTO antes de ejecutar el método
            @Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(
            // Dispara la validación de las restricciones del DTO antes de ejecutar el método
            @Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request);
    }
}
