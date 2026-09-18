package com.oiab.minispi.transfer.service;

import com.oiab.minispi.transfer.TransferStatus;
import com.oiab.minispi.transfer.dto.AccountInfo;
import com.oiab.minispi.transfer.dto.TransferRequest;
import com.oiab.minispi.transfer.dto.TransferResponse;
import com.oiab.minispi.transfer.entity.Account;
import com.oiab.minispi.transfer.entity.Transfer;
import com.oiab.minispi.transfer.repository.AccountRepository;
import com.oiab.minispi.transfer.repository.TransferRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Optional;

@Service
public class TransferService {

    private final TransferRepository transferRepository;
    private final AccountRepository accountRepository;
    private final BankNetworkGateway bankNetworkGateway;

    public TransferService(TransferRepository transferRepository, AccountRepository accountRepository,
                          BankNetworkGateway bankNetworkGateway) {
        this.transferRepository = transferRepository;
        this.accountRepository = accountRepository;
        this.bankNetworkGateway = bankNetworkGateway;
    }

    // Este servicio procesa una transferencia, valida idempotencia, fondos y resuelve la ejecución con retries.
    @Transactional
    public TransferResponse processTransfer(TransferRequest request, String idempotencyKey) {
        String resolvedKey = resolveIdempotencyKey(request, idempotencyKey);
        if (resolvedKey == null || resolvedKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The header X-Idempotency-Key is required.");
        }

        Optional<Transfer> existingTransfer = transferRepository.findByIdempotencyKey(resolvedKey);
        if (existingTransfer.isPresent()) {
            return TransferResponse.fromEntity(existingTransfer.get(), "The transfer already exists with the current idempotency key.");
        }

        validateRequest(request);

        Account source = accountRepository.findByAccountNumber(request.sourceAccount().number())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Source account not found: " + request.sourceAccount().number()));
        Account target = accountRepository.findByAccountNumber(request.targetAccount().number())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Target account not found: " + request.targetAccount().number()));

        if (Objects.equals(source.getAccountNumber(), target.getAccountNumber())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The source account and target account must be different.");
        }

        BigDecimal amount = request.amount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The amount must be greater than zero.");
        }

        if (source.getSaldo().compareTo(amount) < 0) {
            Transfer rejectedTransfer = createTransferRecord(request, resolvedKey, TransferStatus.RECHAZADA, amount);
            transferRepository.save(rejectedTransfer);
            return TransferResponse.fromEntity(rejectedTransfer, "Insufficient funds. The transfer was rejected.");
        }

        Transfer pendingTransfer = createTransferRecord(request, resolvedKey, TransferStatus.PENDIENTE, amount);
        transferRepository.save(pendingTransfer);

        try {
            bankNetworkGateway.executeTransfer(request);
            source.setSaldo(source.getSaldo().subtract(amount));
            target.setSaldo(target.getSaldo().add(amount));
            pendingTransfer.setStatus(TransferStatus.EXITOSA);
            pendingTransfer.setUpdatedTimestamp(OffsetDateTime.now(ZoneOffset.UTC));
            accountRepository.save(source);
            accountRepository.save(target);
            transferRepository.save(pendingTransfer);
            return TransferResponse.fromEntity(pendingTransfer, "Transfer completed successfully.");
        } catch (ExternalBankException ex) {
            pendingTransfer.setStatus(TransferStatus.RECHAZADA);
            pendingTransfer.setUpdatedTimestamp(OffsetDateTime.now(ZoneOffset.UTC));
            transferRepository.save(pendingTransfer);
            return TransferResponse.fromEntity(pendingTransfer, "The external network failed and the transfer was rejected.");
        }
    }

    // Esta validación asegura que la cuenta origen, la cuenta destino y el monto cumplan el contrato mínimo del servicio.
    private void validateRequest(TransferRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A transfer request is required.");
        }
        AccountInfo source = request.sourceAccount();
        AccountInfo target = request.targetAccount();

        if (source == null || source.number() == null || source.number().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The source account number is required.");
        }
        if (target == null || target.number() == null || target.number().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The target account number is required.");
        }
        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The transfer amount must be greater than zero.");
        }
    }

    // Este helper centraliza la resolución de la clave de idempotencia, priorizando el header del request.
    private String resolveIdempotencyKey(TransferRequest request, String idempotencyKey) {
        if (request != null && request.idempotencyKey() != null && !request.idempotencyKey().isBlank()) {
            return request.idempotencyKey();
        }
        return idempotencyKey;
    }

    // Este constructor crea una entidad de transferencia con los datos mínimos para el control del flujo.
    private Transfer createTransferRecord(TransferRequest request, String key, TransferStatus status, BigDecimal amount) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return new Transfer(
                request.sourceAccount().number(),
                request.targetAccount().number(),
                key,
                status,
                amount,
                now
        );
    }
}
