package io.alw.css.cashflowconsumer.model.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;

@Embeddable
public record CashflowEntityPK(
        @Column(name = "CASHFLOW_ID", nullable = false)
        Long cashflowId,
        @Column(name = "CASHFLOW_VERSION", nullable = false)
        Integer cashflowVersion
) implements Serializable {
}
