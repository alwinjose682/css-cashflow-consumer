package io.alw.css.cashflowconsumer.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

public class ApplicationStartupEvent implements ApplicationListener<ApplicationReadyEvent> {
    private final static Logger log = LoggerFactory.getLogger(ApplicationStartupEvent.class);

    @Value("${cashflow-generator.start-switch:#{null}}")
    private Boolean startAllCashflowGenerators;
    @Value("${rest.services.cashflow-generator:#{null}}")
    private String cashflowGeneratorUrl;

    private final RestTemplate restTemplate;

    public ApplicationStartupEvent(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (startAllCashflowGenerators == null || !startAllCashflowGenerators) {
            log.info("Cashflow Generators of upstream simulator are not started as per configuration. Cashflow Generators need to be started manually");
            return;
        } else if (cashflowGeneratorUrl == null) {
            log.info("Cashflow Generators of upstream simulator are not started because cashflow generator url is not configured. Cashflow Generators need to be started manually");
            return;
        } else {
            startAllCashflowGenerators();
        }
    }

    private void startAllCashflowGenerators() {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(cashflowGeneratorUrl);
        URI uri = uriBuilder
                .pathSegment("start", "{key}")
                .build("all");

        ResponseEntity<String> response = restTemplate.postForEntity(uri, null, String.class);
        if (response.getStatusCode().is2xxSuccessful()) {
            String outcome = response.getBody();
            log.info("Started ALL Cashflow Generators in upstream/fo-simulator, {}", outcome);
        }
    }
}
