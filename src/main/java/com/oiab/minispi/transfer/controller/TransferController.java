package com.oiab.minispi.transfer.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.oiab.minispi.transfer.dto.TransferRequest;
import com.oiab.minispi.transfer.dto.TransferResponse;
import com.oiab.minispi.transfer.service.TransferService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class TransferController {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping("/transferencias")
    public ResponseEntity<?> processTransfer(
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody Object payload) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "X-Idempotency-Key header is required");
        }

        List<TransferRequest> requests = parseRequests(payload);
        if (requests.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The request body cannot be empty.");
        }

        if (requests.size() == 1) {
            TransferResponse response = transferService.processTransfer(requests.get(0), idempotencyKey);
            return ResponseEntity.ok(response);
        }

        List<TransferResponse> responses = new ArrayList<>();
        for (TransferRequest request : requests) {
            responses.add(transferService.processTransfer(request, idempotencyKey));
        }

        return ResponseEntity.ok(responses);
    }

    private List<TransferRequest> parseRequests(Object payload) {
        if (payload == null) {
            return List.of();
        }

        if (payload instanceof List<?> items) {
            return OBJECT_MAPPER.convertValue(items, new TypeReference<List<TransferRequest>>() {});
        }

        if (payload instanceof Map<?, ?> map) {
            return List.of(OBJECT_MAPPER.convertValue(map, TransferRequest.class));
        }

        return OBJECT_MAPPER.convertValue(payload, new TypeReference<List<TransferRequest>>() {});
    }
}
