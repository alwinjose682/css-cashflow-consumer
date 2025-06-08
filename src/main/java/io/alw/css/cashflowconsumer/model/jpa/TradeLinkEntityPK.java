package io.alw.css.cashflowconsumer.model.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class TradeLinkEntityPK {
    @Column(name = "CF_ID")
    Long cashflowID;

    @Column(name = "CF_VERSION")
    Integer cashflowVersion;

    public TradeLinkEntityPK() {
    }

    public TradeLinkEntityPK(long cashflowID, int cashflowVersion) {
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TradeLinkEntityPK that)) return false;
        return Objects.equals(cashflowID, that.cashflowID) && Objects.equals(cashflowVersion, that.cashflowVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cashflowID, cashflowVersion);
    }
}
