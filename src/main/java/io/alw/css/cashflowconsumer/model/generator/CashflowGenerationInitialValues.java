package io.alw.css.cashflowconsumer.model.generator;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record CashflowGenerationInitialValues(
        @NotNull LocalDate valueDate,
        @Positive long tradeId,
        @Positive long foCashflowId
) {
}
