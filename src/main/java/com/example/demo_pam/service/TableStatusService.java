package com.example.demo_pam.service;

import com.example.demo_pam.entity.TableStatus;
import com.example.demo_pam.exception.ResourceNotFoundException;
import com.example.demo_pam.repository.TableStatusRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class TableStatusService {

    private final TableStatusRepository tableStatusRepository;

    public TableStatusService(TableStatusRepository tableStatusRepository) {
        this.tableStatusRepository = tableStatusRepository;
    }

    public List<TableStatus> findAll() {
        return tableStatusRepository.findAll();
    }

    public TableStatus findById(UUID id) {
        return tableStatusRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Estado de mesa no encontrado: " + id));
    }

    public TableStatus findByName(TableStatus.TableStatusName name) {
        return tableStatusRepository.findAll().stream()
                .filter(status -> status.getName() == name)
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Estado de mesa no encontrado: " + name));
    }

    public TableStatus create(TableStatus.TableStatusName name) {
        TableStatus status = new TableStatus();
        status.setName(name);
        return tableStatusRepository.save(status);
    }

    public TableStatus update(UUID id, TableStatus.TableStatusName name) {
        TableStatus status = findById(id);
        status.setName(name);
        return tableStatusRepository.save(status);
    }

    public void delete(UUID id) {
        TableStatus status = findById(id);
        tableStatusRepository.delete(status);
    }
}
