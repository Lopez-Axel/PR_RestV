package com.example.demo_pam.dto.response;

import com.example.demo_pam.entity.TableStatusHistory;

import java.time.Instant;

public record TableStatusHistoryResponse(
        String statusName,
        Instant changedAt,
        String changedByEmail
) {

    public static TableStatusHistoryResponse from(TableStatusHistory history) {
        return new TableStatusHistoryResponse(
                history.getStatus().getName().name(),
                history.getChangedAt(),
                history.getChangedBy().getEmail()
        );
    }
}
