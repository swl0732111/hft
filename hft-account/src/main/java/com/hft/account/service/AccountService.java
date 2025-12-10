package com.hft.account.service;

import com.hft.common.domain.Account;
import com.hft.common.domain.AccountBalance;
import com.hft.account.repository.AccountBalanceRepository;
import com.hft.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static com.hft.common.domain.AccountType.SPOT;

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
        return getBalance(accountId, asset, SPOT);
    }

    public AccountBalance getBalance(String accountId, String asset, com.hft.common.domain.AccountType type) {
        return accountBalanceRepository.findByAccountIdAndAssetAndType(accountId, asset, type)
                .orElseGet(() -> {
                    // Auto-provision test accounts with large balance (only for SPOT)
                    BigDecimal initialBalance = BigDecimal.ZERO;
                    if (type == SPOT && accountId.startsWith("test-")) {
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
                            .type(type)
                            .availableBalance(initialBalance)
                            .lockedBalance(BigDecimal.ZERO)
                            .build();
                });
    }

    public List<AccountBalance> getAllBalances(String accountId) {
        return accountBalanceRepository.findByAccountId(accountId);
    }

    public Iterable<AccountBalance> getAllBalances() {
        return accountBalanceRepository.findAll();
    }

    @Transactional
    public AccountBalance lockBalance(String accountId, String asset, BigDecimal amount) {
        // Default to SPOT for backward compatibility or specific logic
        return lockBalance(accountId, asset, SPOT, amount);
    }

    @Transactional
    public AccountBalance lockBalance(String accountId, String asset, com.hft.common.domain.AccountType type,
            BigDecimal amount) {
        AccountBalance balance = getBalance(accountId, asset, type);

        if (balance.getAvailableBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient balance in " + type);
        }

        balance.setAvailableBalance(balance.getAvailableBalance().subtract(amount));
        balance.setLockedBalance(balance.getLockedBalance().add(amount));
        return accountBalanceRepository.save(balance);
    }

    @Transactional
    public AccountBalance unlockBalance(String accountId, String asset, BigDecimal amount) {
        return unlockBalance(accountId, asset, SPOT, amount);
    }

    @Transactional
    public AccountBalance unlockBalance(String accountId, String asset, com.hft.common.domain.AccountType type,
            BigDecimal amount) {
        AccountBalance balance = getBalance(accountId, asset, type);
        balance.setLockedBalance(balance.getLockedBalance().subtract(amount));
        balance.setAvailableBalance(balance.getAvailableBalance().add(amount));
        return accountBalanceRepository.save(balance);
    }

    @Transactional
    public AccountBalance deductLockedBalance(String accountId, String asset, BigDecimal amount) {
        return deductLockedBalance(accountId, asset, SPOT, amount);
    }

    @Transactional
    public AccountBalance deductLockedBalance(String accountId, String asset, com.hft.common.domain.AccountType type,
            BigDecimal amount) {
        AccountBalance balance = getBalance(accountId, asset, type);
        balance.setLockedBalance(balance.getLockedBalance().subtract(amount));
        return accountBalanceRepository.save(balance);
    }

    @Transactional
    public AccountBalance creditBalance(String accountId, String asset, BigDecimal amount) {
        // Default deposits go to SPOT
        return creditBalance(accountId, asset, SPOT, amount);
    }

    @Transactional
    public AccountBalance creditBalance(String accountId, String asset, com.hft.common.domain.AccountType type,
            BigDecimal amount) {
        AccountBalance balance = getBalance(accountId, asset, type);
        balance.setAvailableBalance(balance.getAvailableBalance().add(amount));
        return accountBalanceRepository.save(balance);
    }

    @Transactional
    public AccountBalance debitAvailableBalance(String accountId, String asset, com.hft.common.domain.AccountType type,
            BigDecimal amount) {
        AccountBalance balance = getBalance(accountId, asset, type);
        if (balance.getAvailableBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient balance in " + type);
        }
        balance.setAvailableBalance(balance.getAvailableBalance().subtract(amount));
        return accountBalanceRepository.save(balance);
    }

    public BigDecimal getTotalBalance(String asset) {
        BigDecimal available = accountBalanceRepository.sumAvailableBalanceByAsset(asset);
        BigDecimal locked = accountBalanceRepository.sumLockedBalanceByAsset(asset);

        if (available == null)
            available = BigDecimal.ZERO;
        if (locked == null)
            locked = BigDecimal.ZERO;

        return available.add(locked);
    }
}
