package com.oiab.minispi.transfer.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public record TransferRequest(
        @JsonProperty("sourceAccount") @JsonAlias({"sourceAcount"}) AccountInfo sourceAccount,
        @JsonProperty("targetAccount") @JsonAlias({"targetAcount"}) AccountInfo targetAccount,
        @JsonProperty("amount") @JsonAlias({"mount"}) BigDecimal amount,
        @JsonProperty("status") @JsonAlias({"state"}) String status,
        @JsonProperty("createdTimestamp") OffsetDateTime createdTimestamp,
        @JsonProperty("idempotencyKey") @JsonAlias({"idempotency_key"}) String idempotencyKey
) {
    public TransferRequest {
        if (status == null || status.isBlank()) {
            status = "PENDIENTE";
        }
        if (createdTimestamp == null) {
            createdTimestamp = OffsetDateTime.now(ZoneOffset.UTC);
        }
    }
}
