package com.hft.bot.service;

import com.hft.bot.strategy.ArbitrageStrategy;
import com.hft.trading.domain.Order;
import com.hft.trading.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class BotService {

    private final OrderService orderService;
    private final ArbitrageStrategy arbitrageStrategy;
    private final Random random = new Random();

    /**
     * Run trading bot loop every 5 seconds.
     */
    @Scheduled(fixedRate = 5000)
    public void runBot() {
        log.info("Bot scanning for opportunities...");

        // Mock market data
        String symbol = "BTC-USDC";
        double priceA = 50000.0 + (random.nextDouble() * 100 - 50); // 49950 - 50050
        double priceB = 50000.0 + (random.nextDouble() * 100 - 50); // 49950 - 50050

        Optional<Order> opportunity = arbitrageStrategy.checkOpportunity(symbol, priceA, priceB);

        opportunity.ifPresent(order -> {
            log.info("Executing arbitrage trade for {}", symbol);
            try {
                // In a real bot, we would likely use submitOrderAsync
                orderService.submitOrderAsync(order);
            } catch (Exception e) {
                log.error("Failed to execute trade: {}", e.getMessage());
            }
        });
    }
}
