package com.hft.trading.api;

import com.hft.trading.domain.Order;
import com.hft.trading.domain.Trade;
import com.hft.trading.engine.OrderBook;
import com.hft.trading.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<List<Trade>> submitOrder(@RequestBody OrderRequest request) {
        Order order = new Order();
        order.setWalletAddress(request.getWalletAddress());
        order.setSymbol(request.getSymbol());
        order.setSide(request.getSide());
        order.setChain(request.getChain());
        order.setAccountId(request.getAccountId());
        order.setOrderType(request.getOrderType() != null ? request.getOrderType() : Order.OrderType.LIMIT);

        // Set timestamp
        order.setTimestamp(System.currentTimeMillis());

        // Set price and quantity using helper methods to populate both scaled and
        // legacy fields
        if (request.getPrice() != null) {
            order.setPriceFromDouble(request.getPrice());
        }
        if (request.getQuantity() != null) {
            order.setQuantityFromDouble(request.getQuantity());
            order.setInitialQuantityScaled(order.getQuantityScaled());
            order.setInitialQuantity(order.getQuantity());
        }

        // Handle advanced order fields
        if (request.getStopPrice() != null) {
            order.setStopPriceScaled(com.hft.common.util.FixedPointMath.fromDouble(request.getStopPrice()));
        }
        if (request.getDisplayQuantity() != null) {
            order.setDisplayQuantityScaled(com.hft.common.util.FixedPointMath.fromDouble(request.getDisplayQuantity()));
        }

        return ResponseEntity.ok(orderService.submitOrder(order));
    }

    @GetMapping("/book/{symbol}")
    public ResponseEntity<OrderBook> getOrderBook(@PathVariable String symbol) {
        OrderBook book = orderService.getOrderBook(symbol);
        if (book == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(book);
    }
}
