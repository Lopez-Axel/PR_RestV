package com.example.demo_pam.service;

import com.example.demo_pam.dto.request.BusinessSettingsRequest;
import com.example.demo_pam.dto.response.BusinessSettingsDTO;
import com.example.demo_pam.entity.BusinessSettings;
import com.example.demo_pam.repository.BusinessSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BusinessSettingsService {

    private static final Long SINGLE_ROW_ID = 1L;

    private final BusinessSettingsRepository businessSettingsRepository;

    public BusinessSettingsService(BusinessSettingsRepository businessSettingsRepository) {
        this.businessSettingsRepository = businessSettingsRepository;
    }

    // La tabla tiene una sola fila: si aún no existe, devuelve los valores por defecto
    public BusinessSettings get() {
        return businessSettingsRepository.findById(SINGLE_ROW_ID).orElseGet(BusinessSettings::new);
    }

    @Transactional
    public BusinessSettingsDTO update(BusinessSettingsRequest request) {
        BusinessSettings settings = businessSettingsRepository.findById(SINGLE_ROW_ID)
                .orElseGet(() -> {
                    BusinessSettings created = new BusinessSettings();
                    created.setId(SINGLE_ROW_ID);
                    return created;
                });
        settings.setDefaultMarginPercentage(request.defaultMarginPercentage());
        settings.setMinMarginPercentage(request.minMarginPercentage());
        settings.setMaxMarginPercentage(request.maxMarginPercentage());
        if (request.receiptCounter() != null) {
            settings.setReceiptCounter(request.receiptCounter());
        }
        return BusinessSettingsDTO.from(businessSettingsRepository.save(settings));
    }
}