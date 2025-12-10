package com.hft.trading.fix;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import quickfix.*;

@Configuration
@ConditionalOnProperty(name = "fix.enabled", havingValue = "true", matchIfMissing = false)
public class FixConfig {

    @Bean
    @ConditionalOnProperty(name = "fix.enabled", havingValue = "true", matchIfMissing = false)
    public SocketAcceptor socketAcceptor(FixServerApplication application) throws ConfigError {
        // Load configuration from classpath
        SessionSettings settings = new SessionSettings(
                getClass().getClassLoader().getResourceAsStream("quickfix-server.cfg"));
        FileStoreFactory storeFactory = new FileStoreFactory(settings);
        LogFactory logFactory = new FileLogFactory(settings);
        MessageFactory messageFactory = new DefaultMessageFactory();

        SocketAcceptor acceptor = new SocketAcceptor(application, storeFactory, settings, logFactory, messageFactory);

        // Start the acceptor
        try {
            acceptor.start();
        } catch (ConfigError | RuntimeError e) {
            throw new RuntimeException("Failed to start FIX acceptor", e);
        }

        return acceptor;
    }
}
