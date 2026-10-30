package com.example.demo_pam.controller;

import com.example.demo_pam.entity.TableStatus;
import com.example.demo_pam.service.TableStatusService;
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
@RequestMapping("/table-status")
public class TableStatusController {

    private final TableStatusService tableStatusService;

    public TableStatusController(TableStatusService tableStatusService) {
        this.tableStatusService = tableStatusService;
    }

    @GetMapping
    public List<TableStatus> findAll() {
        return tableStatusService.findAll();
    }

    @GetMapping("/{id}")
    public TableStatus findById(@PathVariable UUID id) {
        return tableStatusService.findById(id);
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping
    public TableStatus create(@RequestBody TableStatus.TableStatusName name) {
        return tableStatusService.create(name);
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PutMapping("/{id}")
    public TableStatus update(@PathVariable UUID id, @RequestBody TableStatus.TableStatusName name) {
        return tableStatusService.update(id, name);
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        tableStatusService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
