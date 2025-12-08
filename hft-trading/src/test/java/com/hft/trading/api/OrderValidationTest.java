package com.hft.trading.api;

import com.hft.trading.domain.Order;
import com.hft.trading.engine.MatchingEngine;
import com.hft.trading.event.OrderEvent;
import com.hft.trading.service.OrderService;
import com.lmax.disruptor.RingBuffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

import com.hft.trading.repository.OrderRepository;
import com.hft.account.service.AccountService;
import com.hft.trading.service.RiskControlService;

class OrderValidationTest {

    private OrderService orderService;
    private MatchingEngine matchingEngine;
    private OrderRepository orderRepository;
    private AccountService accountService;
    private RingBuffer ringBuffer;
    private RiskControlService riskControlService;

    @BeforeEach
    void setUp() {
        matchingEngine = Mockito.mock(MatchingEngine.class);
        orderRepository = Mockito.mock(OrderRepository.class);
        accountService = Mockito.mock(AccountService.class);
        ringBuffer = Mockito.mock(RingBuffer.class);
        riskControlService = Mockito.mock(RiskControlService.class);
        orderService = new OrderService(matchingEngine, orderRepository, accountService, ringBuffer,
                riskControlService);
    }

    @Test
    void testValidSolanaAddress() {
        Order order = Order.builder()
                .symbol("SOL-USDC")
                .side(Order.Side.BUY)
                .price(new BigDecimal("100"))
                .quantity(new BigDecimal("1"))
                .chain(Order.Chain.SOLANA)
                .walletAddress("HN7cABqLq46Es1jh92dQQisAq662SmxELLLsHHe4YWrH") // Valid SOL address
                .build();

        assertDoesNotThrow(() -> orderService.submitOrder(order));
    }

    @Test
    void testInvalidSolanaAddress() {
        Order order = Order.builder()
                .symbol("SOL-USDC")
                .side(Order.Side.BUY)
                .price(new BigDecimal("100"))
                .quantity(new BigDecimal("1"))
                .chain(Order.Chain.SOLANA)
                .walletAddress("InvalidAddress")
                .build();

        assertThrows(IllegalArgumentException.class, () -> orderService.submitOrder(order));
    }

    @Test
    void testValidEvmAddress() {
        Order order = Order.builder()
                .symbol("ETH-USDC")
                .side(Order.Side.BUY)
                .price(new BigDecimal("2000"))
                .quantity(new BigDecimal("1"))
                .chain(Order.Chain.POLYGON)
                .walletAddress("0x742d35Cc6634C0532925a3b844Bc454e4438f44e") // Valid EVM address
                .build();

        assertDoesNotThrow(() -> orderService.submitOrder(order));
    }

    @Test
    void testInvalidEvmAddress() {
        Order order = Order.builder()
                .symbol("ETH-USDC")
                .side(Order.Side.BUY)
                .price(new BigDecimal("2000"))
                .quantity(new BigDecimal("1"))
                .chain(Order.Chain.POLYGON)
                .walletAddress("0xInvalid")
                .build();

        assertThrows(IllegalArgumentException.class, () -> orderService.submitOrder(order));
    }
}
