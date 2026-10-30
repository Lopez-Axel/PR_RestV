package com.example.demo_pam.dto.response;

import com.example.demo_pam.entity.BusinessSettings;

import java.math.BigDecimal;

public record BusinessSettingsDTO(
        BigDecimal defaultMarginPercentage,
        BigDecimal minMarginPercentage,
        BigDecimal maxMarginPercentage,
        Long receiptCounter
) {

    public static BusinessSettingsDTO from(BusinessSettings settings) {
        return new BusinessSettingsDTO(
                settings.getDefaultMarginPercentage(),
                settings.getMinMarginPercentage(),
                settings.getMaxMarginPercentage(),
                settings.getReceiptCounter()
        );
    }
}