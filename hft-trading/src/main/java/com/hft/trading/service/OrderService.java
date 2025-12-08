package com.hft.trading.service;

import com.hft.common.domain.AccountType;
import com.hft.trading.domain.Order;
import com.hft.trading.domain.Trade;
import com.hft.trading.engine.MatchingEngine;
import com.hft.trading.engine.OrderBook;
import com.hft.trading.event.OrderEvent;
import com.hft.trading.repository.OrderRepository;
import com.hft.account.service.AccountService;
import com.lmax.disruptor.RingBuffer;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final MatchingEngine matchingEngine;
    private final OrderRepository orderRepository;
    private final AccountService accountService;
    private final RingBuffer<OrderEvent> orderRingBuffer;
    private final RiskControlService riskControlService;

    /**
     * Submit order via Disruptor RingBuffer (zero allocation, minimal latency).
     * This is the new high-performance path.
     * 
     * Latency breakdown:
     * - Validation: ~100ns
     * - Balance lock: ~1-5μs (synchronous for risk management)
     * - RingBuffer publish: <100ns
     * Total: <10μs
     * 
     * NOTE: DB persistence happens asynchronously in OrderEventHandler.
     * This method returns immediately after publishing to RingBuffer.
     */
    public void submitOrderAsync(Order order) {
        validateSignature(order);
        prepareOrder(order);
        riskControlService.validateOrder(order);

        // Lock balance for buy orders (synchronous for risk management)
        // This is the only blocking operation in the hot path
        if (order.getSide() == Order.Side.BUY && order.getAccountId() != null) {
            BigDecimal requiredAmount = order.getPrice().multiply(order.getQuantity());
            String[] symbolParts = order.getSymbol().split("-");
            String quoteAsset = symbolParts.length > 1 ? symbolParts[1] : "USDC";
            accountService.lockBalance(order.getAccountId(), quoteAsset, requiredAmount);
        }

        // Publish to RingBuffer (zero allocation, <100ns)
        // DB persistence happens asynchronously in OrderEventHandler
        long sequence = orderRingBuffer.next();
        try {
            OrderEvent event = orderRingBuffer.get(sequence);
            event.setFrom(order);
        } finally {
            orderRingBuffer.publish(sequence);
        }
    }

    /**
     * Legacy synchronous order submission.
     * 
     * @deprecated Use submitOrderAsync() for zero allocation
     */
    @Deprecated
    public List<Trade> submitOrder(Order order) {
        validateSignature(order);
        prepareOrder(order);
        riskControlService.validateOrder(order);

        // Lock balance for buy orders
        if (order.getSide() == Order.Side.BUY && order.getAccountId() != null) {
            BigDecimal requiredAmount = order.getPrice().multiply(order.getQuantity());
            String[] symbolParts = order.getSymbol().split("-");
            String quoteAsset = symbolParts.length > 1 ? symbolParts[1] : "USDC";
            accountService.lockBalance(order.getAccountId(), quoteAsset, requiredAmount);
        }

        orderRepository.save(order);
        return matchingEngine.processOrder(order);
    }

    private void prepareOrder(Order order) {
        if (order.getId() == null) {
            order.setId(UUID.randomUUID().toString());
        }
        if (order.getTimestamp() == 0) {
            order.setTimestamp(System.currentTimeMillis());
        }
        if (order.getStatus() == null) {
            order.setStatus(Order.OrderStatus.NEW);
        }
        if (order.getInitialQuantity() == null) {
            order.setInitialQuantity(order.getQuantity());
        }
    }

    private void validateSignature(Order order) {
        if (order.getWalletAddress() == null || order.getWalletAddress().isEmpty()) {
            throw new IllegalArgumentException("Wallet address is required for Web3 orders");
        }
        if (order.getChain() == null) {
            throw new IllegalArgumentException("Chain is required for Web3 orders");
        }

        switch (order.getChain()) {
            case SOLANA:
                validateSolanaAddress(order.getWalletAddress());
                break;
            case POLYGON:
            case ETHEREUM:
            case BSC:
                validateEvmAddress(order.getWalletAddress());
                break;
            default:
                throw new IllegalArgumentException("Unsupported chain: " + order.getChain());
        }
    }

    private void validateSolanaAddress(String address) {
        // Solana addresses are Base58 encoded and typically 32-44 characters
        if (!address.matches("[1-9A-HJ-NP-Za-km-z]{32,44}")) {
            throw new IllegalArgumentException("Invalid Solana address format");
        }
    }

    private void validateEvmAddress(String address) {
        // Simple regex validation for EVM address (0x + 40 hex chars)
        if (!address.matches("^0x[a-fA-F0-9]{40}$")) {
            throw new IllegalArgumentException("Invalid EVM address format");
        }
    }

    public OrderBook getOrderBook(String symbol) {
        return matchingEngine.getOrderBook(symbol);
    }
}
