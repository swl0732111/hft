package com.hft.aggregator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = { "com.hft.aggregator", "com.hft.common" })
public class HftAggregatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(HftAggregatorApplication.class, args);
    }
}
