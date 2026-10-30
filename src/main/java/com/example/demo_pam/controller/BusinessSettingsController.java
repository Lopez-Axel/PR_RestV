package com.example.demo_pam.controller;

import com.example.demo_pam.dto.request.BusinessSettingsRequest;
import com.example.demo_pam.dto.response.BusinessSettingsDTO;
import com.example.demo_pam.service.BusinessSettingsService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/business-settings")
public class BusinessSettingsController {

    private final BusinessSettingsService businessSettingsService;

    public BusinessSettingsController(BusinessSettingsService businessSettingsService) {
        this.businessSettingsService = businessSettingsService;
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping
    public BusinessSettingsDTO get() {
        return BusinessSettingsDTO.from(businessSettingsService.get());
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PutMapping
    public BusinessSettingsDTO update(@Valid @RequestBody BusinessSettingsRequest request) {
        return businessSettingsService.update(request);
    }
}