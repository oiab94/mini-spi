package com.oiab.minispi.transfer.service;

import com.oiab.minispi.transfer.TransferStatus;
import com.oiab.minispi.transfer.dto.AccountInfo;
import com.oiab.minispi.transfer.dto.TransferRequest;
import com.oiab.minispi.transfer.dto.TransferResponse;
import com.oiab.minispi.transfer.entity.Account;
import com.oiab.minispi.transfer.entity.Transfer;
import com.oiab.minispi.transfer.repository.AccountRepository;
import com.oiab.minispi.transfer.repository.TransferRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private BankNetworkGateway bankNetworkGateway;

    @InjectMocks
    private TransferService transferService;

    @Test
    void processTransferReturnsExistingTransferForDuplicateIdempotencyKey() {
        String key = "dup-key";
        Transfer existing = new Transfer(
                "ACC-000001",
                "ACC-000002",
                key,
                TransferStatus.EXITOSA,
                new BigDecimal("150.00"),
                OffsetDateTime.now()
        );

        when(transferRepository.findByIdempotencyKey(key)).thenReturn(Optional.of(existing));

        TransferRequest request = new TransferRequest(
                new AccountInfo("ACC-000001", "Alice"),
                new AccountInfo("ACC-000002", "Bob"),
                new BigDecimal("150.00"),
                "PENDIENTE",
                OffsetDateTime.now(),
                key
        );

        TransferResponse response = transferService.processTransfer(request, key);

        assertEquals("EXITOSA", response.status());
        verify(accountRepository, never()).findByAccountNumber(any());
    }

    @Test
    void processTransferRejectsWhenSourceAccountHasInsufficientFunds() {
        String key = "insufficient-key";
        Account source = new Account("ACC-000001", new BigDecimal("100.00"), OffsetDateTime.now());
        Account target = new Account("ACC-000002", new BigDecimal("500.00"), OffsetDateTime.now());
        TransferRequest request = new TransferRequest(
                new AccountInfo("ACC-000001", "Alice"),
                new AccountInfo("ACC-000002", "Bob"),
                new BigDecimal("200.00"),
                "PENDIENTE",
                OffsetDateTime.now(),
                key
        );

        when(transferRepository.findByIdempotencyKey(key)).thenReturn(Optional.empty());
        when(accountRepository.findByAccountNumber("ACC-000001")).thenReturn(Optional.of(source));
        when(accountRepository.findByAccountNumber("ACC-000002")).thenReturn(Optional.of(target));
        when(transferRepository.save(any(Transfer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransferResponse response = transferService.processTransfer(request, key);

        assertEquals("RECHAZADA", response.status());
        assertEquals("Insufficient funds. The transfer was rejected.", response.message());
        verify(bankNetworkGateway, never()).executeTransfer(any(TransferRequest.class));
    }

    @Test
    void processTransferSucceedsWhenBalancesAreValid() {
        String key = "success-key";
        Account source = new Account("ACC-000001", new BigDecimal("1000.00"), OffsetDateTime.now());
        Account target = new Account("ACC-000002", new BigDecimal("500.00"), OffsetDateTime.now());
        TransferRequest request = new TransferRequest(
                new AccountInfo("ACC-000001", "Alice"),
                new AccountInfo("ACC-000002", "Bob"),
                new BigDecimal("250.00"),
                "PENDIENTE",
                OffsetDateTime.now(),
                key
        );

        when(transferRepository.findByIdempotencyKey(key)).thenReturn(Optional.empty());
        when(accountRepository.findByAccountNumber("ACC-000001")).thenReturn(Optional.of(source));
        when(accountRepository.findByAccountNumber("ACC-000002")).thenReturn(Optional.of(target));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transferRepository.save(any(Transfer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransferResponse response = transferService.processTransfer(request, key);

        assertEquals("EXITOSA", response.status());
        assertEquals(new BigDecimal("750.00"), source.getSaldo());
        assertEquals(new BigDecimal("750.00"), target.getSaldo());
    }
}
