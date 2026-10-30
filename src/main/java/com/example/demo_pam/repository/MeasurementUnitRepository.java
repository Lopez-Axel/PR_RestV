package com.example.demo_pam.repository;

import com.example.demo_pam.entity.MeasurementUnit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MeasurementUnitRepository extends JpaRepository<MeasurementUnit, UUID> {
}