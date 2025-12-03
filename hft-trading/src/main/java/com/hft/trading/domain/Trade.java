package com.hft.trading.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Trade {
    private String makerOrderId;
    private String takerOrderId;
    private BigDecimal price;
    private BigDecimal quantity;
    private long timestamp;
}
