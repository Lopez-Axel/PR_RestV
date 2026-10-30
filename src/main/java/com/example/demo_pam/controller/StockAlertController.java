package com.example.demo_pam.controller;

import com.example.demo_pam.dto.response.StockAlertOverviewDTO;
import com.example.demo_pam.service.StockAlertService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ingredients/alerts")
public class StockAlertController {

    private final StockAlertService stockAlertService;

    public StockAlertController(StockAlertService stockAlertService) {
        this.stockAlertService = stockAlertService;
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'CHEF')")
    @GetMapping
    public StockAlertOverviewDTO getOverview() {
        return stockAlertService.getOverview();
    }
}
