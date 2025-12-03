package com.hft.trading.config;

import com.hft.trading.engine.OffHeapOrderStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class OffHeapConfig {

    @Bean
    public OffHeapOrderStore offHeapOrderStore() throws IOException {
        String dataDir = System.getProperty("user.home") + "/.hft/data";

        // Ensure data directory exists
        java.io.File dir = new java.io.File(dataDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        return new OffHeapOrderStore(dataDir + "/orders.dat", 1_000_000);
    }
}
