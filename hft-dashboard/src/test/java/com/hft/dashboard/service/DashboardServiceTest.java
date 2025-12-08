package com.hft.dashboard.service;

import com.hft.dashboard.dto.TradeHistoryDTO;
import com.hft.trading.domain.Order;
import com.hft.trading.domain.TransactionLog;
import com.hft.trading.repository.OrderRepository;
import com.hft.trading.repository.TransactionLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private TransactionLogRepository transactionLogRepository;

    // Mock other dependencies to avoid null pointers if constructor injection is
    // strict,
    // though @InjectMocks might handle it if we only test getTradeHistory.
    // However, DashboardService has 5 dependencies.
    @Mock
    private com.hft.account.repository.AccountRepository accountRepository;
    @Mock
    private com.hft.trading.repository.TradingVolumeStatsRepository volumeStatsRepository;
    @Mock
    private com.hft.trading.service.TierService tierService;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void getTradeHistory_ShouldReturnSortedHistory() {
        String accountId = "test-account";
        String orderId1 = "order-1";
        String orderId2 = "order-2";

        Order order1 = Order.builder()
                .id(orderId1)
                .accountId(accountId)
                .symbol("BTC/USDT")
                .side(Order.Side.BUY)
                .build();
        order1.setPriceFromDouble(50000.0);
        order1.setQuantityFromDouble(1.0);

        Order order2 = Order.builder()
                .id(orderId2)
                .accountId(accountId)
                .symbol("ETH/USDT")
                .side(Order.Side.SELL)
                .build();
        order2.setPriceFromDouble(3000.0);
        order2.setQuantityFromDouble(10.0);

        TransactionLog log1 = TransactionLog.builder()
                .id("tx-1")
                .orderId(orderId1)
                .symbol("BTC/USDT")
                .eventType(TransactionLog.EventType.ORDER_FILLED)
                .timestamp(1000L)
                .build();

        TransactionLog log2 = TransactionLog.builder()
                .id("tx-2")
                .orderId(orderId2)
                .symbol("ETH/USDT")
                .eventType(TransactionLog.EventType.ORDER_FILLED)
                .timestamp(2000L) // Newer
                .build();

        when(orderRepository.findByAccountId(accountId)).thenReturn(Arrays.asList(order1, order2));
        when(transactionLogRepository.findByOrderIdIn(anyList())).thenReturn(Arrays.asList(log1, log2));

        List<TradeHistoryDTO> result = dashboardService.getTradeHistory(accountId);

        assertEquals(2, result.size());

        // Should be sorted by timestamp descending (log2 is newer)
        assertEquals("tx-2", result.get(0).getTransactionId());
        assertEquals("tx-1", result.get(1).getTransactionId());

        assertEquals("ETH/USDT", result.get(0).getSymbol());
        assertEquals("SELL", result.get(0).getType());
        assertEquals(3000.0, result.get(0).getPrice());

        assertEquals("BTC/USDT", result.get(1).getSymbol());
        assertEquals("BUY", result.get(1).getType());
        assertEquals(50000.0, result.get(1).getPrice());
    }

    @Test
    void getTradeHistory_ShouldHandleNoOrders() {
        String accountId = "empty-account";
        when(orderRepository.findByAccountId(accountId)).thenReturn(Collections.emptyList());
        // transactionLogRepository won't be called if orders are empty, or called with
        // empty list
        // Depending on implementation.
        // Our impl: List<String> orderIds = orders.stream()...;
        // transactionLogRepository.findByOrderIdIn(orderIds);
        // If orderIds is empty, we expect empty result.
        when(transactionLogRepository.findByOrderIdIn(Collections.emptyList())).thenReturn(Collections.emptyList());

        List<TradeHistoryDTO> result = dashboardService.getTradeHistory(accountId);

        assertTrue(result.isEmpty());
    }
}
