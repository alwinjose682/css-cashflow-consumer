package io.alw.css.cashflowconsumer.model.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class TradeLinkEntityPK {
    @Column(name = "CF_ID")
    Long cashflowId;

    @Column(name = "CF_VERSION")
    Integer cashflowVersion;

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TradeLinkEntityPK that)) return false;
        return Objects.equals(cashflowId, that.cashflowId) && Objects.equals(cashflowVersion, that.cashflowVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cashflowId, cashflowVersion);
    }
}
