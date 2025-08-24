package io.alw.css.cashflowconsumer.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.alw.css.cashflowconsumer.model.FoCashflowIDAndTradeID;
import io.alw.css.cashflowconsumer.model.generator.CashflowGenerationInitialValues;
import io.alw.css.cashflowconsumer.model.generator.CashflowGeneratorStartResponse;
import io.alw.css.cashflowconsumer.repository.CashflowRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;

public class ApplicationStartupEvent implements ApplicationListener<ApplicationReadyEvent> {
    private final static Logger log = LoggerFactory.getLogger(ApplicationStartupEvent.class);

    @Value("${cashflow-generator.start-switch:#{null}}")
    private Boolean startAllCashflowGenerators;
    @Value("${rest.services.cashflow-generator:#{null}}")
    private String cashflowGeneratorUrl;

    private final RestTemplate restTemplate;
    private final CashflowRepository cashflowRepository;
    private final ObjectMapper objectMapper;

    public ApplicationStartupEvent(RestTemplate restTemplate, CashflowRepository cashflowRepository, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.cashflowRepository = cashflowRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (startAllCashflowGenerators == null || !startAllCashflowGenerators) {
            log.info("Cashflow Generators of upstream simulator are not started as per configuration. Cashflow Generators need to be started manually");
        } else if (cashflowGeneratorUrl == null) {
            log.info("Cashflow Generators of upstream simulator are not started because cashflow generator url is not configured. Cashflow Generators need to be started manually");
        } else {
            try {
                startAllCashflowGenerators();
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void startAllCashflowGenerators() throws JsonProcessingException {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(cashflowGeneratorUrl);
        URI uri = uriBuilder
                .pathSegment("start", "{key}")
                .build("all");

        var initValues = getCashflowGenerationInitialValues();
        var req = RequestEntity
                .put(uri)
                .accept(MediaType.APPLICATION_JSON)
                .body(initValues);
        var res = restTemplate.exchange(req, CashflowGeneratorStartResponse.class);
        if (res.getStatusCode().is2xxSuccessful()) {
            var outcome = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(res.getBody()); // Converting the response to json again to write a readable friendly string

            log.info("Started ALL Cashflow Generators in upstream/fo-simulator, {}", outcome);
        } else {
            log.warn("Failed to start Cashflow Generators in upstream/fo-simulator. Check the upstream simulator logs for details. REST Response: {}", res.getStatusCode());
        }
    }

    private CashflowGenerationInitialValues getCashflowGenerationInitialValues() {
        FoCashflowIDAndTradeID maxIds = cashflowRepository.findMaxFoCashflowIdAndTradeId();
        if (maxIds == null) {
            var initValues = new CashflowGenerationInitialValues(LocalDate.now(), 1054321L, 15432L);
            log.info("Staring Cashflow Generators with initial values: {}", initValues);
            return initValues;
        }

        var initValues = new CashflowGenerationInitialValues(LocalDate.now(), 1L + maxIds.tradeID(), 1L + maxIds.foCashflowID());
        log.info("Staring Cashflow Generators with initial values greater than the values of last processed cashflow: {}", initValues);
        return initValues;
    }
}
