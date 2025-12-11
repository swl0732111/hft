package com.hft.trading.service;

import com.hft.account.service.AccountService;
import com.hft.common.domain.AccountBalance;
import com.hft.common.util.FixedPointMath;
import com.hft.trading.domain.Order;
import com.hft.trading.event.TradeEvent;
import com.hft.trading.repository.OrderRepository;
import com.hft.trading.state.AccountStateStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {

        @Mock
        private AccountService accountService;
        @Mock
        private OrderRepository orderRepository;
        @Mock
        private FeeService feeService;
        @Mock
        private AccountStateStore accountStateStore;

        private SettlementService settlementService;

        @BeforeEach
        void setUp() {
                settlementService = new SettlementService(accountService, orderRepository, feeService,
                                accountStateStore);
        }

        @Test
        void settleTrade_ShouldExchangeAssetsAndDeductFees() {
                // Setup trade params
                // BTC-USDC: Price 100, Qty 1.0
                String symbol = "BTC-USDC";
                long priceScaled = FixedPointMath.fromDouble(BigDecimal.valueOf(100).doubleValue());
                long qtyScaled = FixedPointMath.fromDouble(BigDecimal.ONE.doubleValue());
                long tradeValueScaled = FixedPointMath.fromDouble(BigDecimal.valueOf(100).doubleValue());

                // Setup Orders
                // Maker: SELLER (Sells 1 BTC)
                Order makerOrder = Order.builder()
                                .id("maker-sell-1")
                                .accountId("seller-1")
                                .symbol(symbol)
                                .side(Order.Side.SELL)
                                .quantityScaled(qtyScaled)
                                .build();

                // Taker: BUYER (Buys 1 BTC)
                Order takerOrder = Order.builder()
                                .id("taker-buy-1")
                                .accountId("buyer-1")
                                .symbol(symbol)
                                .side(Order.Side.BUY)
                                .quantityScaled(qtyScaled)
                                .build();

                // Setup Event
                TradeEvent event = new TradeEvent();
                event.set("maker-sell-1", "taker-buy-1", symbol, priceScaled, qtyScaled, System.currentTimeMillis());

                // Mocks
                when(orderRepository.findById("maker-sell-1")).thenReturn(Optional.of(makerOrder));
                when(orderRepository.findById("taker-buy-1")).thenReturn(Optional.of(takerOrder));

                // Mock Fee: 0.1% (10 bps)
                // Seller fee (on 100 USDC) = 0.1 USDC
                long sellerFeeScaled = FixedPointMath.fromDouble(BigDecimal.valueOf(0.1).doubleValue());
                when(feeService.calculateFeeScaled("seller-1", symbol, tradeValueScaled, true))
                                .thenReturn(sellerFeeScaled);

                // Buyer fee (on 1 BTC) = 0.001 BTC
                long buyerFeeScaled = FixedPointMath.fromDouble(BigDecimal.valueOf(0.001).doubleValue());
                when(feeService.calculateFeeScaled("buyer-1", symbol, qtyScaled, false)).thenReturn(buyerFeeScaled);

                // Mock Account Balance Returns (just to avoid NPEs if used)
                when(accountService.deductLockedBalance(anyString(), anyString(), any()))
                                .thenReturn(AccountBalance.builder().build());
                when(accountService.creditBalance(anyString(), anyString(), any()))
                                .thenReturn(AccountBalance.builder().build());

                // Execution
                settlementService.settleTrade(event);

                // Verification - Seller (Maker)
                // 1. Pays Base (BTC) - Locked
                verify(accountService).deductLockedBalance(
                                eq("seller-1"),
                                eq("BTC"),
                                argThat(bd -> bd.compareTo(BigDecimal.ONE) == 0) // 1.0 BTC
                );
                // 2. Receives Quote (USDC) - Fee
                // Received: 100 - 0.1 = 99.9
                verify(accountService).creditBalance(
                                eq("seller-1"),
                                eq("USDC"),
                                argThat(bd -> bd.compareTo(BigDecimal.valueOf(99.9)) == 0));

                // Verification - Buyer (Taker)
                // 1. Pays Quote (USDC) - Locked
                verify(accountService).deductLockedBalance(
                                eq("buyer-1"),
                                eq("USDC"),
                                argThat(bd -> bd.compareTo(BigDecimal.valueOf(100)) == 0));
                // 2. Receives Base (BTC) - Fee
                // Received: 1.0 - 0.001 = 0.999
                verify(accountService).creditBalance(
                                eq("buyer-1"),
                                eq("BTC"),
                                argThat(bd -> bd.compareTo(BigDecimal.valueOf(0.999)) == 0));
        }
}
