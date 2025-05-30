package io.alw.css.cashflowconsumer.model;

public record NostroDetails(
        String nostroID,
        int nostroVersion,
        String entityCode,
        String currCode,
        String secondaryLedgerAccount,
        boolean primary,
        boolean activeNostro,
        OverridableNostroDetails overridableNostroDetails // Can be null if no overridable nostro is present
) {
}
