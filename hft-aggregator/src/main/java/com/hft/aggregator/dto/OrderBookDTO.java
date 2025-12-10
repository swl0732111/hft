package com.hft.aggregator.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class OrderBookDTO {
    private String symbol;
    private List<LevelDTO> bids;
    private List<LevelDTO> asks;
    private long timestamp;

    @Data
    @Builder
    public static class LevelDTO {
        private double price;
        private double quantity;
        private String source;
    }
}
