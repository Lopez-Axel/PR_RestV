package com.example.demo_pam.repository;

import com.example.demo_pam.entity.TableStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TableStatusHistoryRepository extends JpaRepository<TableStatusHistory, UUID> {

    List<TableStatusHistory> findByTableIdOrderByChangedAtDesc(UUID tableId);
}
