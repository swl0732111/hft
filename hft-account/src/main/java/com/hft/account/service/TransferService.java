package com.hft.account.service;

import com.hft.common.domain.AccountType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService {

    private final AccountService accountService;

    @Transactional
    public void transfer(String accountId, String asset, AccountType fromType, AccountType toType, BigDecimal amount) {
        if (fromType == toType) {
            throw new IllegalArgumentException("Cannot transfer to the same account type");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive");
        }

        log.info("Transferring {} {} from {} to {} for account {}", amount, asset, fromType, toType, accountId);

        // 1. Debit from source
        accountService.debitAvailableBalance(accountId, asset, fromType, amount);

        // 2. Credit to target
        accountService.creditBalance(accountId, asset, toType, amount);

        log.info("Transfer completed successfully");
    }
}
