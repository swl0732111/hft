package com.hft.trading.service;

import com.hft.common.util.FixedPointMath;
import com.hft.trading.domain.Order;
import com.hft.trading.domain.Trade;
import com.hft.trading.domain.TransactionLog;
import com.hft.trading.repository.TransactionLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionLogService {
        private final TransactionLogRepository transactionLogRepository;

        public void logOrderPlaced(Order order) {
                TransactionLog log = TransactionLog.builder()
                                .id(UUID.randomUUID().toString())
                                .eventType(TransactionLog.EventType.ORDER_PLACED)
                                .orderId(order.getId())
                                .symbol(order.getSymbol())
                                .timestamp(System.currentTimeMillis())
                                .details(String.format("Order placed: side=%s, price=%s, quantity=%s",
                                                order.getSide(), order.getPrice(), order.getQuantity()))
                                .build();
                transactionLogRepository.save(log);
        }

        public void logOrderMatched(Trade trade) {
                logOrderMatched(trade.getMakerOrderId(), trade.getTakerOrderId(), "",
                                FixedPointMath.fromDouble(trade.getPrice().doubleValue()),
                                FixedPointMath.fromDouble(trade.getQuantity().doubleValue()));
        }

        public void logOrderMatched(String makerOrderId, String takerOrderId, String symbol, long priceScaled,
                        long quantityScaled) {
                TransactionLog log = TransactionLog.builder()
                                .id(UUID.randomUUID().toString())
                                .eventType(TransactionLog.EventType.ORDER_MATCHED)
                                .orderId(takerOrderId)
                                .symbol(symbol)
                                .timestamp(System.currentTimeMillis())
                                .details(String.format("Trade executed: maker=%s, taker=%s, price=%s, quantity=%s",
                                                makerOrderId, takerOrderId,
                                                FixedPointMath.toDouble(priceScaled),
                                                FixedPointMath.toDouble(quantityScaled)))
                                .build();
                transactionLogRepository.save(log);
        }

        public void logOrderFilled(Order order) {
                TransactionLog log = TransactionLog.builder()
                                .id(UUID.randomUUID().toString())
                                .eventType(TransactionLog.EventType.ORDER_FILLED)
                                .orderId(order.getId())
                                .symbol(order.getSymbol())
                                .timestamp(System.currentTimeMillis())
                                .details(String.format("Order filled: initialQuantity=%s", order.getInitialQuantity()))
                                .build();
                transactionLogRepository.save(log);
        }

        public void logOrderCanceled(String orderId, String symbol) {
                TransactionLog log = TransactionLog.builder()
                                .id(UUID.randomUUID().toString())
                                .eventType(TransactionLog.EventType.ORDER_CANCELED)
                                .orderId(orderId)
                                .symbol(symbol)
                                .timestamp(System.currentTimeMillis())
                                .details("Order canceled")
                                .build();
                transactionLogRepository.save(log);
        }
}
