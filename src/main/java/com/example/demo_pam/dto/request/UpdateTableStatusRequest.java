package com.example.demo_pam.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record UpdateTableStatusRequest(
        @NotNull(message = "El id del estado es obligatorio")
        UUID statusId
) {
}
