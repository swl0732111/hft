package com.hft.account;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
        "com.hft.common"
})
public class AccountApplication {
    public static void main(String[] args) {
        // Explicitly specify configuration location to ensure account's application.yml
        // is loaded instead of being overridden by other modules' configurations
        SpringApplication app = new SpringApplication(AccountApplication.class);
        app.setDefaultProperties(java.util.Map.of(
                "spring.config.name", "application",
                "spring.config.location", "classpath:/application.yml"));
        app.run(args);
    }
}