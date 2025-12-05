package com.hft.trading.event;

import com.hft.common.util.FixedPointMath;
import com.hft.trading.domain.Order;
import com.hft.trading.domain.TradingVolumeStats;
import com.hft.trading.engine.IcebergOrderHandler;
import com.hft.trading.engine.MarketPriceTracker;
import com.hft.trading.engine.StopLimitOrderHandler;
import com.hft.trading.fix.MarketDataPublisher;
import com.hft.trading.persistence.ChronicleTradeQueue;
import com.hft.trading.repository.OrderRepository;
import com.hft.trading.repository.TradingVolumeStatsRepository;
import com.hft.trading.service.FeeService;
import com.hft.trading.service.TransactionLogService;
import com.lmax.disruptor.EventHandler;
import java.time.LocalDate;
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
  private final TradingVolumeStatsRepository volumeStatsRepository;
  private final ChronicleTradeQueue chronicleTradeQueue;
    private final MarketPriceTracker priceTracker;
    private final StopLimitOrderHandler stopLimitOrderHandler;
    private final IcebergOrderHandler icebergOrderHandler;

    @Override
    public void onEvent(TradeEvent event, long sequence, boolean endOfBatch) {
    // Persist to Chronicle Queue first (~200ns)
    try {
      chronicleTradeQueue.append(event);
    } catch (Exception e) {
      log.error("Failed to append trade to Chronicle Queue: {}", event.getSymbol(), e);
    }

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

    // Track trading volume for tier calculation
    trackVolume(event);

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

      // Deduct maker fee (tier-aware, zero-allocation calculation)
      if (makerOrder.getAccountId() != null) {
        long makerFeeScaled =
            feeService.calculateFeeScaled(
                makerOrder.getAccountId(), event.getSymbol(), tradeValueScaled, true);
        java.math.BigDecimal makerFee =
            java.math.BigDecimal.valueOf(FixedPointMath.toDouble(makerFeeScaled));
                feeService.deductMakerFee(makerOrder.getAccountId(), event.getSymbol(),
                        makerFee, quoteAsset);
            }

      // Deduct taker fee (tier-aware)
      if (takerOrder.getAccountId() != null) {
        long takerFeeScaled =
            feeService.calculateFeeScaled(
                takerOrder.getAccountId(), event.getSymbol(), tradeValueScaled, false);
        java.math.BigDecimal takerFee =
            java.math.BigDecimal.valueOf(FixedPointMath.toDouble(takerFeeScaled));
                feeService.deductTakerFee(takerOrder.getAccountId(), event.getSymbol(),
                        takerFee, quoteAsset);
            }

        } catch (Exception e) {
            log.error("Failed to deduct fees for trade: {}", event.getSymbol(), e);
        }
    }

  /**
   * Track trading volume for tier calculation. Updates daily volume stats for both maker and taker
   * accounts.
   */
  private void trackVolume(TradeEvent event) {
    try {
      // Get maker and taker orders
      Order makerOrder = orderRepository.findById(event.getMakerOrderId()).orElse(null);
      Order takerOrder = orderRepository.findById(event.getTakerOrderId()).orElse(null);

      if (makerOrder == null || takerOrder == null) {
        return;
      }

      // Calculate trade value
      long tradeValueScaled =
          FixedPointMath.multiply(event.getPriceScaled(), event.getQuantityScaled());

      LocalDate today = LocalDate.now();

      // Update maker volume
      if (makerOrder.getAccountId() != null) {
        updateAccountVolume(makerOrder.getAccountId(), today, tradeValueScaled);
      }

      // Update taker volume
      if (takerOrder.getAccountId() != null) {
        updateAccountVolume(takerOrder.getAccountId(), today, tradeValueScaled);
      }

    } catch (Exception e) {
      log.error("Failed to track volume for trade: {}", event.getSymbol(), e);
    }
  }

  /** Update volume stats for an account. */
  private void updateAccountVolume(String accountId, LocalDate date, long volumeScaled) {
    TradingVolumeStats stats =
        volumeStatsRepository
            .findByAccountIdAndDate(accountId, date)
            .orElseGet(() -> TradingVolumeStats.createForToday(accountId));

    stats.addVolume(volumeScaled);
    volumeStatsRepository.save(stats);

    log.debug(
        "Volume updated for account {}: +{} (total: {})",
        accountId,
        volumeScaled,
        stats.getVolumeScaled());
  }

    private void publishToFix(TradeEvent event) {
        try {
      // Publish trade to FIX market data feed
      if (marketDataPublisher != null) {
        marketDataPublisher.publishTrade(
            event.getSymbol(),
            event.getPriceScaled(),
            event.getQuantityScaled(),
            event.getTimestamp(),
            event.getMakerOrderId(),
            event.getTakerOrderId());

        log.debug(
            "Trade published to FIX: {} @ {} qty {}",
            event.getSymbol(),
            event.getPriceScaled(),
            event.getQuantityScaled());
      } else {
        // FIX publisher not configured, skip
        log.trace("FIX publisher not available, skipping trade publication");
      }
        } catch (Exception e) {
      log.error("Failed to publish trade to FIX: {}", event.getSymbol(), e);
      // Don't fail the entire trade processing if FIX publish fails
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
