package com.hft.trading.repository;

import com.hft.common.domain.UserTier;
import com.hft.trading.domain.TieredFeeConfig;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/** Repository for tiered fee configurations. */
@Repository
public interface TieredFeeConfigRepository extends CrudRepository<TieredFeeConfig, String> {

  /** Find fee config for a specific symbol and tier. */
  Optional<TieredFeeConfig> findBySymbolAndTierAndActive(
      String symbol, UserTier tier, boolean active);

  /** Find all fee configs for a symbol. */
  List<TieredFeeConfig> findBySymbolAndActive(String symbol, boolean active);

  /** Find all fee configs for a tier. */
  List<TieredFeeConfig> findByTierAndActive(UserTier tier, boolean active);

  /** Find all active fee configs. */
  List<TieredFeeConfig> findByActive(boolean active);
}
