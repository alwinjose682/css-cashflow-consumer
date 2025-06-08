package io.alw.css.cashflowconsumer.config;

import io.alw.css.cashflowconsumer.model.properties.SuppressionConfig;
import io.alw.css.cashflowconsumer.processor.CashflowEnricher;
import io.alw.css.cashflowconsumer.processor.CashflowVersionManager;
import io.alw.css.cashflowconsumer.repository.CashflowRejectionRepository;
import io.alw.css.cashflowconsumer.repository.CashflowRepository;
import io.alw.css.cashflowconsumer.repository.CashflowStore;
import io.alw.css.cashflowconsumer.service.CacheService;
import io.alw.css.dbshared.tx.TXRO;
import io.alw.css.dbshared.tx.TXRW;
import org.apache.ignite.configuration.ClientConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableConfigurationProperties
@EnableJpaRepositories(basePackages = "io.alw.css.cashflowconsumer.repository")
@EntityScan(basePackages = "io.alw.css.cashflowconsumer.model.jpa")
public class AppConfig {

    @Bean
    public CacheService cacheService(ClientConfiguration clientConfiguration) {
        return new CacheService(clientConfiguration);
    }

    @Bean
    public CashflowVersionManager cashflowVersionService(CashflowStore cashflowStore, TXRW txrw, TXRO txro) {
        return new CashflowVersionManager(cashflowStore, txrw, txro);
    }

    @Bean
    public CashflowEnricher cashflowEnricher(SuppressionConfig suppressionConfig, CacheService cacheService) {
        return new CashflowEnricher(suppressionConfig, cacheService);
    }

    @Bean
    public CashflowStore cashflowStore(CashflowRepository cashflowRepository, CashflowRejectionRepository cashflowRejectionRepository) {
        return new CashflowStore(cashflowRepository, cashflowRejectionRepository);
    }
}
