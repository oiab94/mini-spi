package com.oiab.minispi.transfer.entity;

import com.oiab.minispi.transfer.TransferStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "transferencias")
public class Transfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_source_number", nullable = false, length = 50)
    private String sourceAccountNumber;

    @Column(name = "account_target_number", nullable = false, length = 50)
    private String targetAccountNumber;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 255)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TransferStatus status;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "created_timestamp", nullable = false)
    private OffsetDateTime createdTimestamp;

    @Column(name = "updated_timestamp", nullable = false)
    private OffsetDateTime updatedTimestamp;

    public Transfer() {
    }

    public Transfer(String sourceAccountNumber, String targetAccountNumber, String idempotencyKey,
                   TransferStatus status, BigDecimal amount, OffsetDateTime createdTimestamp) {
        this.sourceAccountNumber = sourceAccountNumber;
        this.targetAccountNumber = targetAccountNumber;
        this.idempotencyKey = idempotencyKey;
        this.status = status;
        this.amount = amount;
        this.createdTimestamp = createdTimestamp;
        this.updatedTimestamp = createdTimestamp;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSourceAccountNumber() {
        return sourceAccountNumber;
    }

    public void setSourceAccountNumber(String sourceAccountNumber) {
        this.sourceAccountNumber = sourceAccountNumber;
    }

    public String getTargetAccountNumber() {
        return targetAccountNumber;
    }

    public void setTargetAccountNumber(String targetAccountNumber) {
        this.targetAccountNumber = targetAccountNumber;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public TransferStatus getStatus() {
        return status;
    }

    public void setStatus(TransferStatus status) {
        this.status = status;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public OffsetDateTime getCreatedTimestamp() {
        return createdTimestamp;
    }

    public void setCreatedTimestamp(OffsetDateTime createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }

    public OffsetDateTime getUpdatedTimestamp() {
        return updatedTimestamp;
    }

    public void setUpdatedTimestamp(OffsetDateTime updatedTimestamp) {
        this.updatedTimestamp = updatedTimestamp;
    }
}
