package com.hft.account.api.internal;

import com.hft.account.service.AccountService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Slf4j
@RestController
@RequestMapping("/internal/accounts")
@RequiredArgsConstructor
public class InternalAccountController {

    private final AccountService accountService;

    @PostMapping("/credit")
    public ResponseEntity<Void> creditBalance(@RequestBody CreditRequest request) {
        log.info("Internal credit request: {} {} for account {}", request.getAmount(), request.getAsset(),
                request.getAccountId());
        accountService.creditBalance(request.getAccountId(), request.getAsset(), request.getAmount());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/total-balance/{asset}")
    public ResponseEntity<BigDecimal> getTotalBalance(@PathVariable String asset) {
        BigDecimal total = accountService.getTotalBalance(asset);
        return ResponseEntity.ok(total);
    }

    @Data
    public static class CreditRequest {
        private String accountId;
        private String asset;
        private BigDecimal amount;
    }
}
