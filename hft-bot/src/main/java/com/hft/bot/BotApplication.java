package com.hft.bot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = { "com.hft.bot", "com.hft.common", "com.hft.trading", "com.hft.account" })
public class BotApplication {

    public static void main(String[] args) {
        SpringApplication.run(BotApplication.class, args);
    }
}
