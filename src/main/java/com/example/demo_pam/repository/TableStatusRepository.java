package com.example.demo_pam.repository;

import com.example.demo_pam.entity.TableStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TableStatusRepository extends JpaRepository<TableStatus, UUID> {
}
