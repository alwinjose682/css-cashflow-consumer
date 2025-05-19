package io.alw.css.cashflowconsumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(scanBasePackages = "io.alw.css.cashflowconsumer")
@ConfigurationPropertiesScan("io.alw.css.cashflowconsumer.model.properties")
public class CashflowConsumer {
    public static void main(String[] args) {
        SpringApplication.run(CashflowConsumer.class, args);
    }
}
