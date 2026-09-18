package com.oiab.minispi.transfer.repository;

import com.oiab.minispi.transfer.entity.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TransferRepository extends JpaRepository<Transfer, Long> {
    Optional<Transfer> findByIdempotencyKey(String idempotencyKey);
}
