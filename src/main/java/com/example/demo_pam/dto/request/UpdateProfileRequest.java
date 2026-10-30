package com.example.demo_pam.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
        String firstName,

        @Size(max = 100, message = "El apellido no puede superar 100 caracteres")
        String lastName
) {
}
