package io.alw.css.cashflowconsumer.model;

public record PrimaryNostro(
        String nostroID,
        int nostroVersion,
        String entityCode,
        String currCode,
        String secondaryLedgerAccount,
        boolean primary,
        boolean activeNostro
) {
}
