package com.hft.trading.repository;

import com.hft.trading.domain.TradingVolumeStats;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Repository for trading volume statistics. */
@Repository
public interface TradingVolumeStatsRepository extends CrudRepository<TradingVolumeStats, String> {

  /** Find stats for a specific account and date. */
  Optional<TradingVolumeStats> findByAccountIdAndDate(String accountId, LocalDate date);

  /** Find all stats for an account within a date range. */
  List<TradingVolumeStats> findByAccountIdAndDateBetween(
      String accountId, LocalDate startDate, LocalDate endDate);

  /** Calculate total volume for an account within a date range. */
  @Query(
      "SELECT COALESCE(SUM(volume_scaled), 0) FROM trading_volume_stats "
          + "WHERE account_id = :accountId AND date BETWEEN :startDate AND :endDate")
  long sumVolumeByAccountIdAndDateBetween(
      @Param("accountId") String accountId,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate);

  /** Delete old stats before a certain date (for cleanup). */
  void deleteByDateBefore(LocalDate date);
}
