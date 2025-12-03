package com.hft.trading.event;

import com.hft.common.util.FixedPointMath;
import com.hft.trading.fix.MarketDataPublisher;
import com.hft.trading.domain.Order;
import com.hft.trading.engine.IcebergOrderHandler;
import com.hft.trading.engine.MarketPriceTracker;
import com.hft.trading.engine.StopLimitOrderHandler;
import com.hft.trading.service.FeeService;
import com.hft.trading.service.TransactionLogService;
import com.hft.trading.repository.OrderRepository;
import com.lmax.disruptor.EventHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Event handler for processing trade events from RingBuffer.
 * Publishes trades to FIX, persistence, etc.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TradeEventHandler implements EventHandler<TradeEvent> {
    private final MarketDataPublisher marketDataPublisher;
    private final TransactionLogService transactionLogService;
    private final FeeService feeService;
    private final OrderRepository orderRepository;
    private final MarketPriceTracker priceTracker;
    private final StopLimitOrderHandler stopLimitOrderHandler;
    private final IcebergOrderHandler icebergOrderHandler;

    @Override
    public void onEvent(TradeEvent event, long sequence, boolean endOfBatch) {
        // Update market price tracker
        priceTracker.updatePrice(event.getSymbol(), event.getPriceScaled());

        // Check and trigger stop-limit orders
        stopLimitOrderHandler.checkAndTrigger(event.getSymbol());

        // Check if maker order is iceberg and needs replenishment
        if (icebergOrderHandler.isIcebergOrder(event.getMakerOrderId())) {
            icebergOrderHandler.replenishIcebergOrder(event.getMakerOrderId(), event.getQuantityScaled());
        }

        // Deduct fees for maker and taker
        deductFees(event);

        // Publish to FIX market data feed
        publishToFix(event);

        // Log trade for audit
        logTrade(event);

        // Clear event for reuse (optional)
        // event.clear();
    }

    private void deductFees(TradeEvent event) {
        try {
            // Get maker and taker orders
            Order makerOrder = orderRepository.findById(event.getMakerOrderId()).orElse(null);
            Order takerOrder = orderRepository.findById(event.getTakerOrderId()).orElse(null);

            if (makerOrder == null || takerOrder == null) {
                log.warn("Order not found for fee deduction: maker={}, taker={}",
                        event.getMakerOrderId(), event.getTakerOrderId());
                return;
            }

            // Calculate trade value using scaled arithmetic
            long tradeValueScaled = FixedPointMath.multiply(
                    event.getPriceScaled(), event.getQuantityScaled());

            // Extract quote asset from symbol (e.g., BTC-USDC -> USDC)
            String[] symbolParts = event.getSymbol().split("-");
            String quoteAsset = symbolParts.length > 1 ? symbolParts[1] : "USDC";

            // Deduct maker fee (zero-allocation calculation)
            long makerFeeScaled = feeService.calculateFeeScaled(event.getSymbol(), tradeValueScaled, true);
            java.math.BigDecimal makerFee = java.math.BigDecimal.valueOf(
                    FixedPointMath.toDouble(makerFeeScaled));

            if (makerOrder.getAccountId() != null) {
                feeService.deductMakerFee(makerOrder.getAccountId(), event.getSymbol(),
                        makerFee, quoteAsset);
            }

            // Deduct taker fee
            long takerFeeScaled = feeService.calculateFeeScaled(event.getSymbol(), tradeValueScaled, false);
            java.math.BigDecimal takerFee = java.math.BigDecimal.valueOf(
                    FixedPointMath.toDouble(takerFeeScaled));

            if (takerOrder.getAccountId() != null) {
                feeService.deductTakerFee(takerOrder.getAccountId(), event.getSymbol(),
                        takerFee, quoteAsset);
            }

            log.debug("Fees deducted - Maker: {} {}, Taker: {} {}",
                    makerFee, quoteAsset, takerFee, quoteAsset);

        } catch (Exception e) {
            log.error("Failed to deduct fees for trade: {}", event.getSymbol(), e);
        }
    }

    private void publishToFix(TradeEvent event) {
        try {
            // TODO: Integrate with actual FIX publisher
            // marketDataPublisher.publishTrade(...);
            log.debug("Trade published: {} @ {} qty {}",
                    event.getSymbol(), event.getPriceScaled(), event.getQuantityScaled());
        } catch (Exception e) {
            log.error("Failed to publish trade to FIX", e);
        }
    }

    private void logTrade(TradeEvent event) {
        try {
            transactionLogService.logOrderMatched(
                    event.getMakerOrderId(),
                    event.getTakerOrderId(),
                    event.getSymbol(),
                    event.getPriceScaled(),
                    event.getQuantityScaled());
        } catch (Exception e) {
            log.error("Failed to log trade", e);
        }
    }
}
