package com.hft.account.api;

import com.hft.common.domain.AccountType;
import com.hft.account.service.TransferService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @PostMapping
    public ResponseEntity<Void> transfer(@RequestBody TransferRequest request) {
        transferService.transfer(
                request.getAccountId(),
                request.getAsset(),
                request.getFromType(),
                request.getToType(),
                request.getAmount());
        return ResponseEntity.ok().build();
    }

    @Data
    public static class TransferRequest {
        private String accountId;
        private String asset;
        private AccountType fromType;
        private AccountType toType;
        private BigDecimal amount;
    }
}
