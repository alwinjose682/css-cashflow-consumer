package io.alw.css.cashflowconsumer.processor;

import io.alw.css.domain.cashflow.Cashflow;

public sealed interface CFProcessedCheckOutcome {
    AlreadyProcessed ALREADY_PROCESSED = new AlreadyProcessed();
    FirstVersion FIRST_VERSION = new FirstVersion();

    record AlreadyProcessed() implements CFProcessedCheckOutcome {
    }

    record FirstVersion() implements CFProcessedCheckOutcome {
    }

    record NonFirstVersion(Cashflow lastProcessedCashflow) implements CFProcessedCheckOutcome {
    }

    record LastCashflowIsCancelled() implements CFProcessedCheckOutcome {
    }
}
