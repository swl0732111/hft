package com.hft.aggregator.disruptor;

import com.hft.aggregator.aggregator.OrderBookAggregator;
import com.lmax.disruptor.EventHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Single-threaded consumer that updates the Aggregator.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventHandler implements EventHandler<OrderEvent> {

    private final OrderBookAggregator aggregator;

    @Override
    public void onEvent(OrderEvent event, long sequence, boolean endOfBatch) {
        if (event.isBid()) {
            aggregator.updateBid(event.getSymbolId(), event.getPrice(), event.getQuantity(), event.getSource());
        } else {
            aggregator.updateAsk(event.getSymbolId(), event.getPrice(), event.getQuantity(), event.getSource());
        }
    }
}
