package com.hft.dashboard.service;

import com.hft.dashboard.dto.TradeHistoryDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class MockDashboardServiceTest {

    @InjectMocks
    private MockDashboardService mockDashboardService;

    @Test
    void getTradeHistory_ShouldReturnMockDataWithoutNPE() {
        String accountId = "test-account";

        // This call previously caused NPE because it called super.getTradeHistory()
        // which tried to access null orderRepository.
        List<TradeHistoryDTO> result = mockDashboardService.getTradeHistory(accountId);

        assertNotNull(result);
        assertEquals(20, result.size()); // We generate 20 mock trades

        TradeHistoryDTO trade = result.get(0);
        assertNotNull(trade.getTransactionId());
        assertNotNull(trade.getSymbol());
        assertTrue(trade.getPrice() > 0);
    }
}
