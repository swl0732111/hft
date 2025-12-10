package com.hft.aggregator.bootstrap;

import com.hft.aggregator.aggregator.OrderBookAggregator;
import com.hft.aggregator.domain.SymbolDictionary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
@Slf4j
@RequiredArgsConstructor
public class DemoDataSeeder implements CommandLineRunner {

    private final OrderBookAggregator aggregator;
    private final Random random = new Random();

    // Base prices
    private double btcPrice = 98000.0;
    private double ethPrice = 3800.0;
    private double solPrice = 145.0;

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting Demo Data Seeder...");
        // Initialize symbols
        SymbolDictionary.getOrCreateId("BTC-USDC");
        SymbolDictionary.getOrCreateId("ETH-USDC");
        SymbolDictionary.getOrCreateId("SOL-USDC");
        SymbolDictionary.getOrCreateId("BTC/USDT");
    }

    @Scheduled(fixedRate = 1000)
    public void simulateMarketData() {
        updateSymbol("BTC-USDC", btcPrice);
        updateSymbol("ETH-USDC", ethPrice);
        updateSymbol("SOL-USDC", solPrice);
        updateSymbol("BTC/USDT", btcPrice);

        // Random walk
        btcPrice += (random.nextDouble() - 0.5) * 50;
        ethPrice += (random.nextDouble() - 0.5) * 5;
        solPrice += (random.nextDouble() - 0.5) * 0.5;
    }

    private void updateSymbol(String symbol, double basePrice) {
        int id = SymbolDictionary.getOrCreateId(symbol);
        double spread = basePrice * 0.0001; // 1 bps
        long priceScale = 100_000_000L;

        long bidPx = (long) ((basePrice - spread / 2) * priceScale);
        long askPx = (long) ((basePrice + spread / 2) * priceScale);
        long qty = 1_000_000_000L; // 10 units scaled

        aggregator.updateBid(id, bidPx, qty, "Binance");
        aggregator.updateAsk(id, askPx, qty, "Binance");
    }
}
