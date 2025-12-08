package com.hft.trading.repository;

import com.hft.trading.domain.Trade;
import org.springframework.data.repository.CrudRepository;

public interface TradeRepository extends CrudRepository<Trade, String> {
}
