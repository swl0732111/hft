package com.hft.trading.repository;

import com.hft.trading.domain.Order;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface OrderRepository extends CrudRepository<Order, String> {
    List<Order> findBySymbolAndQuantityGreaterThan(String symbol, BigDecimal quantity);
}
