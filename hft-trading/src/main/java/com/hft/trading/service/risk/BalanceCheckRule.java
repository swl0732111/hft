package com.hft.trading.service.risk;

import com.hft.account.service.AccountService;
import com.hft.common.domain.AccountBalance;
import com.hft.trading.domain.Order;
import com.hft.trading.state.AccountStateStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class BalanceCheckRule implements RiskRule {

    private final AccountService accountService;
    private final AccountStateStore accountStateStore;

    @Override
    public void validate(Order order) {
        String symbol = order.getSymbol();
        String[] assets = symbol.split("-");
        if (assets.length != 2) {
            log.warn("Invalid symbol format: {}", symbol);
            return; // Skip validation for invalid symbols
        }

        String baseAsset = assets[0];
        String quoteAsset = assets[1];
        String accountId = order.getAccountId();

        if (order.getSide() == Order.Side.BUY) {
            // Buying Base, paying with Quote
            // Cost = Price * Quantity
            BigDecimal cost = BigDecimal.valueOf(order.getPriceAsDouble())
                    .multiply(BigDecimal.valueOf(order.getQuantityAsDouble()));

            AccountBalance balance = getBalance(accountId, quoteAsset);
            if (balance.getAvailableBalance().compareTo(cost) < 0) {
                throw new IllegalArgumentException(String.format(
                        "Insufficient funds: Required %s %s, Available %s %s",
                        cost, quoteAsset, balance.getAvailableBalance(), quoteAsset));
            }
        } else {
            // Selling Base, receiving Quote
            // Need Quantity of Base
            BigDecimal requiredQuantity = BigDecimal.valueOf(order.getQuantityAsDouble());

            AccountBalance balance = getBalance(accountId, baseAsset);
            if (balance.getAvailableBalance().compareTo(requiredQuantity) < 0) {
                throw new IllegalArgumentException(String.format(
                        "Insufficient funds: Required %s %s, Available %s %s",
                        requiredQuantity, baseAsset, balance.getAvailableBalance(), baseAsset));
            }
        }
    }

    private AccountBalance getBalance(String accountId, String asset) {
        // Try RocksDB first
        AccountBalance balance = accountStateStore.getBalance(accountId, asset);
        if (balance != null) {
            return balance;
        }

        // On cache miss, load ALL balances for this user (lazy loading strategy)
        // This way, subsequent checks for other assets will hit cache
        log.debug("Cache miss for account {}, loading all balances for this user", accountId);
        loadUserBalances(accountId);

        // Now try again from cache
        balance = accountStateStore.getBalance(accountId, asset);
        if (balance != null) {
            return balance;
        }

        // If still not found, fetch specific balance from DB
        balance = accountService.getBalance(accountId, asset);
        if (balance != null) {
            accountStateStore.updateBalance(balance);
        }
        return balance;
    }

    /**
     * Load all balances for a user into cache.
     * This is called on first cache miss for the user.
     */
    private void loadUserBalances(String accountId) {
        try {
            var allBalances = accountService.getAllBalances(accountId);
            int loaded = 0;
            for (AccountBalance bal : allBalances) {
                accountStateStore.updateBalance(bal);
                loaded++;
            }
            log.info("Loaded {} balances for user {} into cache", loaded, accountId);
        } catch (Exception e) {
            log.warn("Failed to load all balances for user {}: {}", accountId, e.getMessage());
        }
    }
}
