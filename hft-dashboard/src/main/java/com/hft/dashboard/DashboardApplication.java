package com.hft.dashboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import com.hft.account.repository.AccountRepository;
import com.hft.trading.repository.TradingVolumeStatsRepository;
import com.hft.trading.repository.TierConfigRepository;
import com.hft.trading.service.TierService;

/** Dashboard application main class. */
@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = { "com.hft.dashboard", "com.hft.common", "com.hft.wallet", "com.hft.account" })
@EnableJdbcRepositories(basePackages = { "com.hft.trading.repository", "com.hft.account.repository",
    "com.hft.wallet.repository" })
public class DashboardApplication {

  public static void main(String[] args) {
    // Explicitly specify configuration location to ensure dashboard's
    // application.yml
    // is loaded instead of being overridden by hft-trading's application.properties
    SpringApplication app = new SpringApplication(DashboardApplication.class);
    app.setDefaultProperties(java.util.Map.of(
        "spring.config.name", "application",
        "spring.config.location", "classpath:/application.yml"));
    app.run(args);
  }

  /**
   * Manually create TierService bean to avoid scanning entire
   * com.hft.trading.service package.
   * This prevents unwanted beans (OrderService, FeeService, etc.) from being
   * loaded.
   */
  @Bean
  public TierService tierService(
      AccountRepository accountRepository,
      TradingVolumeStatsRepository volumeStatsRepository,
      TierConfigRepository tierConfigRepository) {
    return new TierService(accountRepository, volumeStatsRepository, tierConfigRepository);
  }
}
