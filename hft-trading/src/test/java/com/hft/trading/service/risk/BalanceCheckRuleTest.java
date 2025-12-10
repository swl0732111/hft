package com.hft.trading.service.risk;

import com.hft.account.service.AccountService;
import com.hft.common.domain.AccountBalance;
import com.hft.trading.domain.Order;
import com.hft.trading.state.AccountStateStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class BalanceCheckRuleTest {

    @Mock
    private AccountService accountService;

    @Mock
    private AccountStateStore accountStateStore;

    private BalanceCheckRule balanceCheckRule;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        balanceCheckRule = new BalanceCheckRule(accountService, accountStateStore);
    }

    @Test
    void validate_ShouldPass_WhenBuyOrderHasSufficientFunds() {
        Order order = Order.builder()
                .symbol("BTC-USDC")
                .side(Order.Side.BUY)
                .accountId("user1")
                .build();
        order.setPriceFromDouble(50000.0);
        order.setQuantityFromDouble(1.0);

        AccountBalance balance = AccountBalance.builder()
                .availableBalance(new BigDecimal("60000.0"))
                .build();

        when(accountService.getBalance("user1", "USDC")).thenReturn(balance);

        assertDoesNotThrow(() -> balanceCheckRule.validate(order));
    }

    @Test
    void validate_ShouldFail_WhenBuyOrderHasInsufficientFunds() {
        Order order = Order.builder()
                .symbol("BTC-USDC")
                .side(Order.Side.BUY)
                .accountId("user1")
                .build();
        order.setPriceFromDouble(50000.0);
        order.setQuantityFromDouble(1.0);

        AccountBalance balance = AccountBalance.builder()
                .availableBalance(new BigDecimal("40000.0"))
                .build();

        when(accountService.getBalance("user1", "USDC")).thenReturn(balance);

        assertThrows(IllegalArgumentException.class, () -> balanceCheckRule.validate(order));
    }

    @Test
    void validate_ShouldPass_WhenSellOrderHasSufficientFunds() {
        Order order = Order.builder()
                .symbol("BTC-USDC")
                .side(Order.Side.SELL)
                .accountId("user1")
                .build();
        order.setPriceFromDouble(50000.0);
        order.setQuantityFromDouble(1.0);

        AccountBalance balance = AccountBalance.builder()
                .availableBalance(new BigDecimal("1.5"))
                .build();

        when(accountService.getBalance("user1", "BTC")).thenReturn(balance);

        assertDoesNotThrow(() -> balanceCheckRule.validate(order));
    }

    @Test
    void validate_ShouldFail_WhenSellOrderHasInsufficientFunds() {
        Order order = Order.builder()
                .symbol("BTC-USDC")
                .side(Order.Side.SELL)
                .accountId("user1")
                .build();
        order.setPriceFromDouble(50000.0);
        order.setQuantityFromDouble(1.0);

        AccountBalance balance = AccountBalance.builder()
                .availableBalance(new BigDecimal("0.5"))
                .build();

        when(accountService.getBalance("user1", "BTC")).thenReturn(balance);

        assertThrows(IllegalArgumentException.class, () -> balanceCheckRule.validate(order));
    }
}
