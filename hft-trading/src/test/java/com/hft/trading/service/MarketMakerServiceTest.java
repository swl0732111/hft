package com.hft.trading.service;

import com.hft.account.service.AccountService;
import com.hft.trading.domain.Order;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarketMakerServiceTest {

    @Mock
    private OrderService orderService;

    @Mock
    private AccountService accountService;

    @InjectMocks
    private MarketMakerService marketMakerService;

    @Test
    void provideLiquidity_ShouldRunCoreLoop() {
        // Run the scheduled task
        marketMakerService.provideLiquidity();

        // 1. Verify Initialization (Credit Funds)
        verify(accountService, times(1)).creditBalance(eq("0"), eq("USDC"), any(BigDecimal.class));
        verify(accountService, times(1)).creditBalance(eq("0"), eq("BTC"), any(BigDecimal.class));

        // 2. Verify Orders Placed (Buy + Sell)
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderService, times(2)).submitOrderAsync(orderCaptor.capture());

        Order sellOrder = orderCaptor.getAllValues().get(0);
        Order buyOrder = orderCaptor.getAllValues().get(1);

        // Verify SELL
        assertEquals(Order.Side.SELL, sellOrder.getSide());
        assertEquals("BTC-USDC", sellOrder.getSymbol());
        assertEquals("0", sellOrder.getAccountId());
        assertEquals(Order.OrderType.LIMIT, sellOrder.getOrderType());

        // Verify BUY
        assertEquals(Order.Side.BUY, buyOrder.getSide());
        assertEquals("BTC-USDC", buyOrder.getSymbol());
        assertEquals("0", buyOrder.getAccountId());
    }

    @Test
    void provideLiquidity_ShouldNotReInitialize() {
        // First run
        marketMakerService.provideLiquidity();

        // Second run
        marketMakerService.provideLiquidity();

        // Verify initialization only happened once
        verify(accountService, times(1)).creditBalance(eq("0"), eq("USDC"), any(BigDecimal.class));
    }
}
