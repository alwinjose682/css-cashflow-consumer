package io.alw.css.cashflowconsumer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

public final class CashflowGeneratorStarter {
    private final static Logger log = LoggerFactory.getLogger(CashflowGeneratorStarter.class);

    public static void startAll(RestTemplate restTemplate, String cashflowGeneratorUrl) {
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
