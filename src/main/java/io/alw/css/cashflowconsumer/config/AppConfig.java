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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableConfigurationProperties
@EnableJpaRepositories(basePackages = "io.alw.css.cashflowconsumer.repository")
@EntityScan(basePackages = "io.alw.css.cashflowconsumer.model.jpa")
// no @EnableTransactionManagement. Declarative tx is not used. Programmatic tx is used instead
public class AppConfig {

    // Explicitly making TXRW and TXRO as spring beans.
    // 'spring.factories' is defined for auto configuration. It does not work for 'cashflow-consumer', but works for 'db-cache-data-loader' where spring-data-jpa is not used
    // Do not know the reason
    @Bean("txro")
    public TXRO txro(PlatformTransactionManager platformTransactionManager) {
        return new TXRO(platformTransactionManager);
    }

    @Bean("txrw")
    public TXRW txrw(PlatformTransactionManager platformTransactionManager) {
        return new TXRW(platformTransactionManager);
    }

    @Bean
    public CacheService cacheService(ClientConfiguration clientConfiguration) {
        return new CacheService(clientConfiguration);
    }

    @Bean
    public CashflowVersionManager cashflowVersionManager(CashflowStore cashflowStore, TXRW txrw, TXRO txro) {
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
