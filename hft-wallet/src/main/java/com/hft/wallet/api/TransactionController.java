package com.hft.wallet.api;

import com.hft.wallet.service.ChainIntegrationService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/chain")
@RequiredArgsConstructor
public class TransactionController {

    private final ChainIntegrationService chainService;

    @GetMapping("/balance/{address}")
    public ResponseEntity<BalanceResponse> getBalance(@PathVariable String address) {
        try {
            BigDecimal balance = chainService.getBalance(address);
            return ResponseEntity.ok(new BalanceResponse(address, balance, "ETH"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/withdraw")
    public CompletableFuture<ResponseEntity<WithdrawResponse>> withdraw(@RequestBody WithdrawRequest request) {
        return chainService.submitWithdrawal(request.getToAddress(), request.getAmount())
                .thenApply(txHash -> ResponseEntity.ok(new WithdrawResponse(txHash, "Withdrawal submitted")))
                .exceptionally(e -> {
                    log.error("Withdrawal failed", e);
                    return ResponseEntity.internalServerError().build();
                });
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BalanceResponse {
        private String address;
        private BigDecimal balance;
        private String currency;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class WithdrawRequest {
        private String toAddress;
        private BigDecimal amount;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class WithdrawResponse {
        private String txHash;
        private String status;
    }
}
