package com.hft.common.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * DataSource configuration optimized for high-throughput persistence.
 */
@Configuration
@EnableScheduling
public class DataSourceConfig {

    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();

        // Connection settings
        config.setJdbcUrl("jdbc:h2:mem:hft;DB_CLOSE_DELAY=-1;MODE=PostgreSQL");
        config.setUsername("sa");
        config.setPassword("");
        config.setDriverClassName("org.h2.Driver");

        // Pool sizing (optimized for high throughput)
        config.setMaximumPoolSize(20);
        config.setMinimumIdle(10);

        // Timeouts
        config.setConnectionTimeout(1000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        // Performance tuning
        config.setAutoCommit(false); // Manual commit for batching
        config.setConnectionTestQuery("SELECT 1");
        config.setPoolName("HFT-HikariPool");

        // Statement caching
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");

        return new HikariDataSource(config);
    }
}
