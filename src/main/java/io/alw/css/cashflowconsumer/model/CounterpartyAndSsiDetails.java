package io.alw.css.cashflowconsumer.model;

import io.alw.css.domain.cashflow.TradeType;

public record CounterpartyAndSsiDetails(
        String counterpartyCode,
        int counterpartyVersion,
        boolean internal,
        boolean activeCounterparty,
        String ssiId,
        int ssiVersion,
        String currCode,
        TradeType product,
        boolean primarySsi,
        boolean activeSsi
) {
}
