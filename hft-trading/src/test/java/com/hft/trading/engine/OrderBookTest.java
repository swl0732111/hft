//package com.hft.trading.engine;
//
//import com.hft.trading.domain.Order;
//import com.hft.trading.domain.Trade;
//import org.junit.jupiter.api.Test;
//
//import java.math.BigDecimal;
//import java.util.List;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//class OrderBookTest {
//
//        @Test
//        void testFullMatch() {
//                OrderBook orderBook = new OrderBook(new InMemoryOrderStore());
//
//                // Add a sell order
//                Order sellOrder = Order.builder()
//                                .id("sell1")
//                                .side(Order.Side.SELL)
//                                .price(new BigDecimal("100"))
//                                .quantity(new BigDecimal("10"))
//                                .timestamp(System.currentTimeMillis())
//                                .build();
//                orderBook.match(sellOrder);
//
//                // Add a buy order that matches fully
//                Order buyOrder = Order.builder()
//                                .id("buy1")
//                                .side(Order.Side.BUY)
//                                .price(new BigDecimal("100"))
//                                .quantity(new BigDecimal("10"))
//                                .timestamp(System.currentTimeMillis())
//                                .build();
//
//                List<Trade> trades = orderBook.match(buyOrder);
//
//                assertEquals(1, trades.size());
//                assertEquals(new BigDecimal("10"), trades.get(0).getQuantity());
//                assertEquals(new BigDecimal("100"), trades.get(0).getPrice());
//                assertTrue(orderBook.getAsks().isEmpty());
//                assertTrue(orderBook.getBids().isEmpty());
//        }
//
//        @Test
//        void testPartialMatch() {
//                OrderBook orderBook = new OrderBook(new InMemoryOrderStore());
//
//                // Add a sell order
//                Order sellOrder = Order.builder()
//                                .id("sell1")
//                                .side(Order.Side.SELL)
//                                .price(new BigDecimal("100"))
//                                .quantity(new BigDecimal("10"))
//                                .timestamp(System.currentTimeMillis())
//                                .build();
//                orderBook.match(sellOrder);
//
//                // Add a buy order that matches partially
//                Order buyOrder = Order.builder()
//                                .id("buy1")
//                                .side(Order.Side.BUY)
//                                .price(new BigDecimal("100"))
//                                .quantity(new BigDecimal("5"))
//                                .timestamp(System.currentTimeMillis())
//                                .build();
//
//                List<Trade> trades = orderBook.match(buyOrder);
//
//                assertEquals(1, trades.size());
//                assertEquals(new BigDecimal("5"), trades.get(0).getQuantity());
//
//                // Remaining sell order should be 5
//                assertEquals(1, orderBook.getAsks().size());
//                assertEquals(new BigDecimal("5"), orderBook.getAsks().get(new BigDecimal("100")).get(0).getQuantity());
//        }
//
//        @Test
//        void testNoMatch() {
//                OrderBook orderBook = new OrderBook(new InMemoryOrderStore());
//
//                // Add a sell order at 100
//                Order sellOrder = Order.builder()
//                                .id("sell1")
//                                .side(Order.Side.SELL)
//                                .price(new BigDecimal("100"))
//                                .quantity(new BigDecimal("10"))
//                                .timestamp(System.currentTimeMillis())
//                                .build();
//                orderBook.match(sellOrder);
//
//                // Add a buy order at 90 (should not match)
//                Order buyOrder = Order.builder()
//                                .id("buy1")
//                                .side(Order.Side.BUY)
//                                .price(new BigDecimal("90"))
//                                .quantity(new BigDecimal("10"))
//                                .timestamp(System.currentTimeMillis())
//                                .build();
//
//                List<Trade> trades = orderBook.match(buyOrder);
//
//                assertTrue(trades.isEmpty());
//                assertEquals(1, orderBook.getAsks().size());
//                assertEquals(1, orderBook.getBids().size());
//        }
//
//        @Test
//        void testCancelOrder() {
//                OrderBook orderBook = new OrderBook(new InMemoryOrderStore()); // Instantiate OrderBook for this test
//                Order order = Order.builder()
//                                .id("1")
//                                .side(Order.Side.BUY)
//                                .price(new BigDecimal("50000"))
//                                .quantity(new BigDecimal("1.0"))
//                                .timestamp(System.currentTimeMillis())
//                                .build();
//
//                orderBook.match(order);
//                assertFalse(orderBook.getBids().isEmpty());
//
//                orderBook.cancelOrder("1");
//                assertTrue(orderBook.getBids().isEmpty());
//        }
//}
