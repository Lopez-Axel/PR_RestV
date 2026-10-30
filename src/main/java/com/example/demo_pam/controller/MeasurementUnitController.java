package com.example.demo_pam.controller;

import com.example.demo_pam.dto.request.MeasurementUnitRequest;
import com.example.demo_pam.dto.response.MeasurementUnitDTO;
import com.example.demo_pam.service.MeasurementUnitService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/measurement-units")
public class MeasurementUnitController {

    private final MeasurementUnitService measurementUnitService;

    public MeasurementUnitController(MeasurementUnitService measurementUnitService) {
        this.measurementUnitService = measurementUnitService;
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'CHEF')")
    @GetMapping
    public List<MeasurementUnitDTO> findAll() {
        return measurementUnitService.findAll();
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'CHEF')")
    @GetMapping("/{id}")
    public MeasurementUnitDTO findById(@PathVariable UUID id) {
        return measurementUnitService.findById(id);
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping
    public MeasurementUnitDTO create(@Valid @RequestBody MeasurementUnitRequest request) {
        return measurementUnitService.create(
                request.name(), request.abbreviation(), request.unitType(), request.conversionFactorToBase());
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PutMapping("/{id}")
    public MeasurementUnitDTO update(@PathVariable UUID id, @Valid @RequestBody MeasurementUnitRequest request) {
        return measurementUnitService.update(
                id, request.name(), request.abbreviation(), request.unitType(), request.conversionFactorToBase());
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        measurementUnitService.delete(id);
        return ResponseEntity.noContent().build();
    }
}