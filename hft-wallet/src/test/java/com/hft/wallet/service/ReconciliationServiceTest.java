package com.hft.wallet.service;

import com.hft.wallet.client.TradingClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class ReconciliationServiceTest {

    @Mock
    private TradingClient tradingClient;

    private ReconciliationService reconciliationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        reconciliationService = new ReconciliationService(tradingClient);
    }

    @Test
    void verifyReserves_ShouldReturnTrue_WhenSolvent() {
        String asset = "USDT";
        // Internal liabilities: 900,000
        when(tradingClient.getTotalBalance(asset)).thenReturn(new BigDecimal("900000"));

        // Mock on-chain assets: 1,000,000 (hardcoded in service for now)

        boolean result = reconciliationService.verifyReserves(asset);

        assertTrue(result, "Reserves should be verified when assets > liabilities");
    }

    @Test
    void verifyReserves_ShouldReturnFalse_WhenInsolvent() {
        String asset = "USDT";
        // Internal liabilities: 1,100,000 (Greater than mock 1,000,000)
        when(tradingClient.getTotalBalance(asset)).thenReturn(new BigDecimal("1100000"));

        boolean result = reconciliationService.verifyReserves(asset);

        assertFalse(result, "Reserves should fail verification when liabilities > assets");
    }
}
