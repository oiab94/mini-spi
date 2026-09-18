package com.oiab.minispi.transfer.dto;

import com.oiab.minispi.transfer.entity.Transfer;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record TransferResponse(
        String idempotencyKey,
        String status,
        String message,
        String sourceAccountNumber,
        String targetAccountNumber,
        BigDecimal amount,
        OffsetDateTime createdTimestamp
) {
    public static TransferResponse fromEntity(Transfer transfer, String message) {
        return new TransferResponse(
                transfer.getIdempotencyKey(),
                transfer.getStatus().name(),
                message,
                transfer.getSourceAccountNumber(),
                transfer.getTargetAccountNumber(),
                transfer.getAmount(),
                transfer.getCreatedTimestamp()
        );
    }
}
