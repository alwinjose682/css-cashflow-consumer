package io.alw.css.cashflowconsumer.model.generator;

import java.util.List;

public record CashflowGeneratorStartResponse(
        List<String> msgs,
        List<GeneratorDetail> startedGenerators,
        List<String> stoppedGenerators,
        List<String> failedGenerators
) {
}
