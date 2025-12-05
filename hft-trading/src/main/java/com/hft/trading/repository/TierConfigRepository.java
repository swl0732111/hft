package com.hft.trading.repository;

import com.hft.common.domain.TierConfig;
import com.hft.common.domain.UserTier;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/** Repository for tier configuration management. */
@Repository
public interface TierConfigRepository extends CrudRepository<TierConfig, String> {

  /** Find tier configuration by tier level. */
  Optional<TierConfig> findByTierAndActive(UserTier tier, boolean active);

  /** Find all active tier configurations. */
  Iterable<TierConfig> findByActive(boolean active);
}
