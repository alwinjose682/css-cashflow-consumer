package io.alw.css.cashflowconsumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

// TODO: jdbc batching, disable auto-commit, disable open-session-in-view. Must be done one at a time
// TODO: MDC logging
@SpringBootApplication(scanBasePackages = "io.alw.css.cashflowconsumer")
@ConfigurationPropertiesScan("io.alw.css.cashflowconsumer.model.properties")
public class CashflowConsumer {
    public static void main(String[] args) {
        SpringApplication.run(CashflowConsumer.class, args);
    }
}
