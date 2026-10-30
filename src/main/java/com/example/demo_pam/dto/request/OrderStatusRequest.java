package com.example.demo_pam.dto.request;

import com.example.demo_pam.entity.OrderStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record OrderStatusRequest(
        @NotNull(message = "El nombre es obligatorio")
        OrderStatus.OrderStatusName name,

        @NotBlank(message = "El color es obligatorio")
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "El color debe tener formato #RRGGBB")
        @Size(max = 7, message = "El color no puede superar los 7 caracteres")
        String colorHex
) {
}