package io.alw.css.cashflowconsumer.model;

public record NostroDetails(
        PrimaryNostro primaryNostro, // Can be null if no primary nostro is present
        OverridableNostro overridableNostro // Can be null if no overridable nostro is present
) {
}
