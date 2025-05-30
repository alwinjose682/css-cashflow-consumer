package io.alw.css.cashflowconsumer.model;

public record OverridableNostroDetails(
        // Counterparty Sla Mapping Details
        long slaMappingID,
        int slaMappingVersion,
        String counterpartyCode,
        // Standard Nostro Details
        String nostroID,
        int nostroVersion,
        String entityCode,
        String currCode,
        String secondaryLedgerAccount,
        boolean primary,
        boolean activeNostro
) {
}
