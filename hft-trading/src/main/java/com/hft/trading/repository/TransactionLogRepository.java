package com.hft.trading.repository;

import com.hft.trading.domain.TransactionLog;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionLogRepository extends CrudRepository<TransactionLog, String> {
    List<TransactionLog> findByOrderId(String orderId);

    List<TransactionLog> findBySymbol(String symbol);

    List<TransactionLog> findByOrderIdIn(List<String> orderIds);
}
