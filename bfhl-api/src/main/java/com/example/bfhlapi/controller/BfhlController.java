package com.example.bfhlapi.controller;

import com.example.bfhlapi.dto.BfhlRequest;
import com.example.bfhlapi.dto.BfhlResponse;
import com.example.bfhlapi.dto.HealthResponse;
import com.example.bfhlapi.service.BfhlService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
public class BfhlController {

    private final BfhlService bfhlService;

    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        return ResponseEntity.ok(new HealthResponse("UP"));
    }

    @PostMapping("/bfhl")
    public ResponseEntity<BfhlResponse> process(
            @RequestHeader("X-Request-Id") @NotBlank(message = "X-Request-Id header is mandatory") String requestId,
            @Valid @RequestBody BfhlRequest request) {

        MDC.put("request_id", requestId);
        try {
            int receivedCount = request.getData() == null ? 0 : request.getData().size();
            log.atInfo()
                    .addKeyValue("requestId", requestId)
                    .addKeyValue("receivedCount", receivedCount)
                    .log("BFHL request received");

            BfhlResponse response = bfhlService.process(request, requestId);

            log.atInfo()
                    .addKeyValue("requestId", requestId)
                    .addKeyValue("processingTimeMs", response.getProcessingTimeMs())
                    .log("BFHL request completed");

            return ResponseEntity.ok(response);
        } finally {
            MDC.remove("request_id");
        }
    }
}
