package com.hft.trading.domain;

import com.hft.common.util.FixedPointMath;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("orders")
public class Order implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    private String id;
    private String accountId;
    private String walletAddress;
    private String symbol;
    private Side side;
    private Chain chain;

    // Legacy BigDecimal fields (deprecated, use scaled fields)
    @Deprecated
    private BigDecimal price;
    @Deprecated
    private BigDecimal quantity;
    @Deprecated
    private BigDecimal initialQuantity;

    // Zero-allocation fixed-point fields (8 decimal places)
    private long priceScaled;
    private long quantityScaled;
    private long initialQuantityScaled;

    private long timestamp;
    private OrderStatus status;

    // Conversion helpers
    public void setPriceFromDouble(double price) {
        this.priceScaled = FixedPointMath.fromDouble(price);
        this.price = BigDecimal.valueOf(price);
    }

    public void setQuantityFromDouble(double quantity) {
        this.quantityScaled = FixedPointMath.fromDouble(quantity);
        this.quantity = BigDecimal.valueOf(quantity);
    }

    public double getPriceAsDouble() {
        return FixedPointMath.toDouble(priceScaled);
    }

    public double getQuantityAsDouble() {
        return FixedPointMath.toDouble(quantityScaled);
    }

    public enum Side {
        BUY, SELL
    }

    public enum Chain {
        SOLANA, POLYGON, ETHEREUM, BSC
    }

    public enum OrderStatus {
        NEW, PARTIALLY_FILLED, FILLED, CANCELED, REJECTED
    }

    public enum OrderType {
        LIMIT, // Standard limit order
        MARKET, // Market order (future)
        ICEBERG, // Iceberg order (hidden quantity)
        STOP_LIMIT // Stop-limit order (triggered at stop price)
    }

    // Order type and advanced features
    private OrderType orderType = OrderType.LIMIT;

    // Iceberg order fields
    private long displayQuantityScaled; // Visible quantity for iceberg orders
    private long hiddenQuantityScaled; // Remaining hidden quantity

    // Stop-limit order fields
    private long stopPriceScaled; // Trigger price for stop-limit orders
    private boolean triggered; // Whether stop order has been triggered
}
