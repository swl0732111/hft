package com.hft.dashboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Dashboard application main class. */
@SpringBootApplication
@EnableScheduling
@ComponentScan(
    basePackages = {"com.hft.dashboard", "com.hft.common", "com.hft.trading", "com.hft.wallet"})
public class DashboardApplication {

  public static void main(String[] args) {
    SpringApplication.run(DashboardApplication.class, args);
  }
}
