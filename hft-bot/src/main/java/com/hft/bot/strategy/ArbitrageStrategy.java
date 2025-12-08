package com.hft.bot.strategy;

import com.hft.trading.domain.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Slf4j
@Component
public class ArbitrageStrategy {

    /**
     * Check for arbitrage opportunity.
     * For demonstration, this uses a simple mock logic:
     * If price on Exchange A < Exchange B, buy on A and sell on B.
     *
     * @param symbol The trading pair symbol
     * @param priceA Price on Exchange A (mocked)
     * @param priceB Price on Exchange B (mocked)
     * @return Optional Order if opportunity exists
     */
    public Optional<Order> checkOpportunity(String symbol, double priceA, double priceB) {
        double spread = priceB - priceA;
        double threshold = 10.0; // Minimum profit threshold

        if (spread > threshold) {
            log.info("Arbitrage opportunity found for {}: Buy at {}, Sell at {}, Spread {}", symbol, priceA, priceB,
                    spread);

            // Create a BUY order on the lower priced exchange (Exchange A)
            // In a real scenario, we would also create a SELL order on Exchange B
            Order order = Order.builder()
                    .symbol(symbol)
                    .side(Order.Side.BUY)
                    .chain(Order.Chain.POLYGON) // Default chain
                    .walletAddress("0xBotWalletAddress") // Bot wallet
                    .build();

            order.setPriceFromDouble(priceA);
            order.setQuantityFromDouble(1.0); // Fixed quantity for demo

            return Optional.of(order);
        }

        return Optional.empty();
    }
}
