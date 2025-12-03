package com.hft.trading.repository;

import com.hft.trading.domain.FeeConfig;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FeeConfigRepository extends CrudRepository<FeeConfig, String> {
    Optional<FeeConfig> findBySymbolAndActive(String symbol, boolean active);
}
