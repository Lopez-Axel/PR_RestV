package com.example.demo_pam.dto.response;

import java.util.List;

public record StockAlertOverviewDTO(
        List<StockAlertDTO> alerts,
        List<StockNotificationDTO> notifications
) {
}
