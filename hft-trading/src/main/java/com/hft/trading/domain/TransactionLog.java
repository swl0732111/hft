package com.hft.trading.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("transaction_log")
public class TransactionLog {
    @Id
    private String id;
    private EventType eventType;
    private String orderId;
    private String symbol;
    private long timestamp;
    private String details;

    public enum EventType {
        ORDER_PLACED, ORDER_MATCHED, ORDER_CANCELED, ORDER_FILLED
    }
}
