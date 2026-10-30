package com.example.demo_pam.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.Map;

// Envoltorio uniforme de toda respuesta de error: mismo esquema en 400/401/403/404/409/413/415/500
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<String> requiredRoles,
        Map<String, String> fieldErrors
) {
}