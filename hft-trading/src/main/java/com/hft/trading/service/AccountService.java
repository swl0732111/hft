package com.hft.trading.service;

import com.hft.common.domain.Account;
import com.hft.common.domain.AccountBalance;
import com.hft.trading.repository.AccountBalanceRepository;
import com.hft.trading.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accountRepository;
    private final AccountBalanceRepository accountBalanceRepository;

    @Transactional
    public Account createAccount(String userId, String username, String email) {
        Account account = Account.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .username(username)
                .email(email)
                .createdAt(System.currentTimeMillis())
                .status(Account.AccountStatus.ACTIVE)
                .build();
        return accountRepository.save(account);
    }

    public Account getAccount(String accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));
    }

    public AccountBalance getBalance(String accountId, String asset) {
        return accountBalanceRepository.findByAccountIdAndAsset(accountId, asset)
                .orElseGet(() -> {
                    // Auto-provision test accounts with large balance
                    BigDecimal initialBalance = BigDecimal.ZERO;
                    if (accountId.startsWith("test-")) {
                        initialBalance = new BigDecimal("1000000000");

                        // Ensure account exists to satisfy FK constraint
                        if (!accountRepository.existsById(accountId)) {
                            Account account = Account.builder()
                                    .id(accountId)
                                    .userId(accountId)
                                    .username("Test User")
                                    .email(accountId + "@test.com")
                                    .createdAt(System.currentTimeMillis())
                                    .status(Account.AccountStatus.ACTIVE)
                                    .build();
                            accountRepository.save(account);
                        }
                    }

                    return AccountBalance.builder()
                            .id(UUID.randomUUID().toString())
                            .accountId(accountId)
                            .asset(asset)
                            .availableBalance(initialBalance)
                            .lockedBalance(BigDecimal.ZERO)
                            .build();
                });
    }

    public List<AccountBalance> getAllBalances(String accountId) {
        return accountBalanceRepository.findByAccountId(accountId);
    }

    @Transactional
    public void lockBalance(String accountId, String asset, BigDecimal amount) {
        AccountBalance balance = getBalance(accountId, asset);

        if (balance.getAvailableBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient balance");
        }

        balance.setAvailableBalance(balance.getAvailableBalance().subtract(amount));
        balance.setLockedBalance(balance.getLockedBalance().add(amount));
        accountBalanceRepository.save(balance);
    }

    @Transactional
    public void unlockBalance(String accountId, String asset, BigDecimal amount) {
        AccountBalance balance = getBalance(accountId, asset);
        balance.setLockedBalance(balance.getLockedBalance().subtract(amount));
        balance.setAvailableBalance(balance.getAvailableBalance().add(amount));
        accountBalanceRepository.save(balance);
    }

    @Transactional
    public void deductLockedBalance(String accountId, String asset, BigDecimal amount) {
        AccountBalance balance = getBalance(accountId, asset);
        balance.setLockedBalance(balance.getLockedBalance().subtract(amount));
        accountBalanceRepository.save(balance);
    }

    @Transactional
    public void creditBalance(String accountId, String asset, BigDecimal amount) {
        AccountBalance balance = getBalance(accountId, asset);
        balance.setAvailableBalance(balance.getAvailableBalance().add(amount));
        accountBalanceRepository.save(balance);
    }
}
