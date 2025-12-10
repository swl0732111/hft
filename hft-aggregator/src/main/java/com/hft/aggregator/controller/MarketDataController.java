package com.hft.aggregator.controller;

import com.hft.aggregator.aggregator.OrderBookAggregator;
import com.hft.aggregator.dto.OrderBookDTO;
import com.hft.aggregator.dto.QuoteDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/market")
@RequiredArgsConstructor
public class MarketDataController {

    private final OrderBookAggregator aggregator;
    private static final double SCALE = 100_000_000.0;

    @GetMapping("/quote/{symbol}")
    public ResponseEntity<QuoteDTO> getQuote(@PathVariable String symbol) {
        // Resolve ID first
        int symbolId = com.hft.aggregator.domain.SymbolDictionary.getId(symbol);
        if (symbolId == -1) {
            return ResponseEntity.notFound().build();
        }

        com.hft.aggregator.domain.ArrayOrderBook book = aggregator.getOrderBook(symbolId);
        if (book == null || (book.getBidCount() == 0 && book.getAskCount() == 0)) {
            return ResponseEntity.notFound().build();
        }

        QuoteDTO quote = QuoteDTO.builder()
                .symbol(symbol)
                .bestBidPx(book.getBestBidPrice() / SCALE)
                .bestBidQty(book.getBestBidQuantity() / SCALE)
                .bestAskPx(book.getBestAskPrice() / SCALE)
                .bestAskQty(book.getBestAskQuantity() / SCALE)
                .spread(book.getSpread() / SCALE)
                .timestamp(System.currentTimeMillis())
                .build();

        return ResponseEntity.ok(quote);
    }

    @GetMapping("/book/{symbol}")
    public ResponseEntity<OrderBookDTO> getOrderBook(@PathVariable String symbol,
            @RequestParam(defaultValue = "10") int depth) {

        int symbolId = com.hft.aggregator.domain.SymbolDictionary.getId(symbol);
        if (symbolId == -1) {
            return ResponseEntity.notFound().build();
        }

        com.hft.aggregator.domain.ArrayOrderBook book = aggregator.getOrderBook(symbolId);
        if (book == null) {
            return ResponseEntity.notFound().build();
        }

        List<OrderBookDTO.LevelDTO> bids = new java.util.ArrayList<>();
        int limit = Math.min(depth, book.getBidCount());
        for (int i = 0; i < limit; i++) {
            bids.add(OrderBookDTO.LevelDTO.builder()
                    .price(book.getBidPrice(i) / SCALE)
                    .quantity(book.getBidQuantity(i) / SCALE)
                    .source("aggregated") // Source tracking lost in simple array book for now (optimized)
                    .build());
        }

        List<OrderBookDTO.LevelDTO> asks = new java.util.ArrayList<>();
        limit = Math.min(depth, book.getAskCount());
        for (int i = 0; i < limit; i++) {
            asks.add(OrderBookDTO.LevelDTO.builder()
                    .price(book.getAskPrice(i) / SCALE)
                    .quantity(book.getAskQuantity(i) / SCALE)
                    .source("aggregated")
                    .build());
        }

        OrderBookDTO dto = OrderBookDTO.builder()
                .symbol(symbol)
                .bids(bids)
                .asks(asks)
                .timestamp(System.currentTimeMillis())
                .build();

        return ResponseEntity.ok(dto);
    }
}
