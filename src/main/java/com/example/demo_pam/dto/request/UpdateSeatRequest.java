package com.example.demo_pam.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateSeatRequest(
        @NotNull(message = "El estado de ocupación es obligatorio")
        Boolean occupied
) {
}
