package io.alw.css.cashflowconsumer.config;

import io.alw.css.cashflowconsumer.model.properties.SuppressionConfig;
import io.alw.css.cashflowconsumer.processor.CashflowEnricher;
import io.alw.css.cashflowconsumer.processor.CashflowVersionManager;
import io.alw.css.cashflowconsumer.repository.CashflowRejectionRepository;
import io.alw.css.cashflowconsumer.repository.CashflowRepository;
import io.alw.css.cashflowconsumer.repository.CashflowStore;
import io.alw.css.cashflowconsumer.service.CacheService;
import io.alw.css.cashflowconsumer.service.CashflowGeneratorStarter;
import io.alw.css.dbshared.tx.TXRO;
import io.alw.css.dbshared.tx.TXRW;
import org.apache.ignite.configuration.ClientConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableConfigurationProperties
@EnableJpaRepositories(basePackages = "io.alw.css.cashflowconsumer.repository")
@EntityScan(basePackages = "io.alw.css.cashflowconsumer.model.jpa")
// no @EnableTransactionManagement. Declarative tx is not used. Programmatic tx is used instead
public class AppConfig {
    private final static Logger log = LoggerFactory.getLogger(AppConfig.class);

    @Value("${cashflow-generator.start-switch:#{null}}")
    private Boolean startAllCashflowGenerators;

    @Value("${rest.services.cashflow-generator:#{null}}")
    private String cashflowGeneratorUrl;

    @EventListener(ApplicationReadyEvent.class)
    void startFoSimulatorGenerators() {
        if (startAllCashflowGenerators == null || !startAllCashflowGenerators) {
            log.info("Cashflow Generators of upstream simulator are not started as per configuration. Cashflow Generators need to be started manually");
            return;
        } else if (cashflowGeneratorUrl == null) {
            log.info("Cashflow Generators of upstream simulator are not started because cashflow generator url is not configured. Cashflow Generators need to be started manually");
            return;
        }

        RestTemplate restTemplate = new RestTemplateBuilder().build();
        CashflowGeneratorStarter.startAll(restTemplate, cashflowGeneratorUrl);
    }

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
