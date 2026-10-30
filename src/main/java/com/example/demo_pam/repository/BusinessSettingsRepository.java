package com.example.demo_pam.repository;

import com.example.demo_pam.entity.BusinessSettings;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface BusinessSettingsRepository extends JpaRepository<BusinessSettings, Long> {

    // Serializa el reparto de folios de recibo entre confirmaciones concurrentes
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from BusinessSettings b where b.id = 1")
    Optional<BusinessSettings> findByIdWithLock();
}