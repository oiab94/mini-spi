package com.oiab.minispi.transfer.service;

import com.oiab.minispi.transfer.dto.TransferRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

@Service
public class BankNetworkGateway {

    private static final Logger log = LoggerFactory.getLogger(BankNetworkGateway.class);

    @Value("${spi.bank.failure-rate:20.0}")
    private double failureRate;

    @Value("${spi.bank.max-retries:3}")
    private int maxRetries;

    // Este gateway simula la red bancaria y realiza reintentos controlados ante fallas temporales.
    public void executeTransfer(TransferRequest request) {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            if (shouldFailRandomly()) {
                log.warn("External bank transfer failed for attempt {} of {} on idempotency key {}.",
                        attempt, maxRetries, request.idempotencyKey());
                if (attempt == maxRetries) {
                    throw new ExternalBankException("The external banking network failed after the third retry.");
                }
                continue;
            }
            return;
        }

        throw new ExternalBankException("The external banking network failed after all configured retries.");
    }

    // Esta validación simula fallas aleatorias para el 20% de las transferencias, con posibilidad de ajuste por configuración.
    private boolean shouldFailRandomly() {
        if (failureRate <= 0.0) {
            return false;
        }
        return ThreadLocalRandom.current().nextDouble(0, 100) < failureRate;
    }
}
