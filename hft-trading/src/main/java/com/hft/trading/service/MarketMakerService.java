package com.hft.trading.service;

import com.hft.account.service.AccountService;
import com.hft.trading.domain.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Random;

@Service
@Slf4j
@RequiredArgsConstructor
public class MarketMakerService {

    private final OrderService orderService;
    private final AccountService accountService; // Needed to give MM money
    private final Random random = new Random();

    // Configuration
    private static final String MM_USER_ID = "0"; // Special System User
    private static final String SYMBOL = "BTC-USDC";
    private static final BigDecimal QUANTITY = new BigDecimal("1.0"); // 1 BTC size

    // State
    private double currentPrice = 50000.0;
    private boolean initialized = false;

    @Scheduled(fixedRate = 3000) // Run every 3 seconds
    public void provideLiquidity() {
        if (!initialized) {
            initializeAccount();
            initialized = true;
        }

        // 1. Update Price (Random Walk)
        updatePrice();

        // 2. Place Orders (Ladder)
        placeOrders();
    }

    private void initializeAccount() {
        // Give the MM infinite money
        accountService.creditBalance(MM_USER_ID, "USDC", new BigDecimal("1000000000"));
        accountService.creditBalance(MM_USER_ID, "BTC", new BigDecimal("10000"));
        log.info("Market Maker account initialized with funds.");
    }

    private void updatePrice() {
        // Random walk: +/- 0.1%
        double change = (random.nextDouble() - 0.5) * 0.002;
        currentPrice = currentPrice * (1 + change);
        log.info("Market Maker updating fair price to: {}", String.format("%.2f", currentPrice));
    }

    private void placeOrders() {
        BigDecimal midPrice = BigDecimal.valueOf(currentPrice);
        BigDecimal spread = new BigDecimal("10.0"); // $10 spread

        BigDecimal bidPrice = midPrice.subtract(spread);
        BigDecimal askPrice = midPrice.add(spread);

        // Place SELL (Ask)
        placeOrder(Order.Side.SELL, askPrice);

        // Place BUY (Bid)
        placeOrder(Order.Side.BUY, bidPrice);
    }

    private void placeOrder(Order.Side side, BigDecimal price) {
        // Use Builder or setters correct
        Order order = new Order();
        order.setId("MM-" + System.nanoTime());
        order.setAccountId(MM_USER_ID); // setUserId -> setAccountId
        order.setSymbol(SYMBOL);
        order.setSide(side);

        // Use double setters for scaled logic
        order.setPriceFromDouble(price.doubleValue());
        order.setQuantityFromDouble(QUANTITY.doubleValue());
        order.setInitialQuantityScaled(order.getQuantityScaled()); // Important!

        order.setOrderType(Order.OrderType.LIMIT); // setType -> setOrderType
        order.setStatus(Order.OrderStatus.NEW);
        order.setTimestamp(System.currentTimeMillis());

        // Use Fire-and-Forget submission
        orderService.submitOrderAsync(order);
    }
}
