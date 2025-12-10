package com.hft.aggregator.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QuoteDTO {
    private String symbol;
    private double bestBidPx;
    private double bestBidQty;
    private double bestAskPx;
    private double bestAskQty;
    private double spread;
    private long timestamp;
}
