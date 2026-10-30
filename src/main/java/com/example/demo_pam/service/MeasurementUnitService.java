package com.example.demo_pam.service;

import com.example.demo_pam.dto.response.MeasurementUnitDTO;
import com.example.demo_pam.entity.MeasurementUnit;
import com.example.demo_pam.exception.ResourceNotFoundException;
import com.example.demo_pam.repository.MeasurementUnitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class MeasurementUnitService {

    private final MeasurementUnitRepository measurementUnitRepository;

    public MeasurementUnitService(MeasurementUnitRepository measurementUnitRepository) {
        this.measurementUnitRepository = measurementUnitRepository;
    }

    public List<MeasurementUnitDTO> findAll() {
        return measurementUnitRepository.findAll().stream().map(MeasurementUnitDTO::from).toList();
    }

    public MeasurementUnitDTO findById(UUID id) {
        return MeasurementUnitDTO.from(getUnit(id));
    }

    @Transactional
    public MeasurementUnitDTO create(String name, String abbreviation,
                                     MeasurementUnit.UnitType unitType, BigDecimal conversionFactorToBase) {
        MeasurementUnit unit = new MeasurementUnit();
        unit.setName(name);
        unit.setAbbreviation(abbreviation);
        unit.setUnitType(unitType);
        unit.setConversionFactorToBase(conversionFactorToBase);
        return MeasurementUnitDTO.from(measurementUnitRepository.save(unit));
    }

    @Transactional
    public MeasurementUnitDTO update(UUID id, String name, String abbreviation,
                                     MeasurementUnit.UnitType unitType, BigDecimal conversionFactorToBase) {
        MeasurementUnit unit = getUnit(id);
        unit.setName(name);
        unit.setAbbreviation(abbreviation);
        unit.setUnitType(unitType);
        unit.setConversionFactorToBase(conversionFactorToBase);
        return MeasurementUnitDTO.from(measurementUnitRepository.save(unit));
    }

    @Transactional
    public void delete(UUID id) {
        measurementUnitRepository.delete(getUnit(id));
    }

    public MeasurementUnit getUnit(UUID id) {
        return measurementUnitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Unidad de medida no encontrada: " + id));
    }
}