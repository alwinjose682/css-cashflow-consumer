package io.alw.css.cashflowconsumer.model.jpa;

import io.alw.css.domain.cashflow.TradeType;
import io.alw.css.domain.common.EventMsgProcessStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "CASHFLOW_CONSUMER_AUDIT", schema = "CSS")
public class CashflowConsumerAuditEntity {
    public CashflowConsumerAuditEntity() {
    }

    public CashflowConsumerAuditEntity(long id, long foCashflowID, int foCashflowVersion, long tradedID, int tradeVersion, TradeType tradeType, EventMsgProcessStatus status, LocalDateTime receivedDateTime) {
        this.id = id;
        this.foCashflowID = foCashflowID;
        this.foCashflowVersion = foCashflowVersion;
        this.tradedID = tradedID;
        this.tradeVersion = tradeVersion;
        this.status = status;
        this.tradeType = tradeType;
    }

    @Id
    @Column(name = "ID")
    Long id;

    @Column(name = "FO_CASHFLOW_ID")
    Long foCashflowID;

    @Column(name = "FO_CASHFLOW_VERSION")
    Integer foCashflowVersion;

    @Column(name = "TRADE_ID")
    Long tradedID;

    @Column(name = "trade_version")
    Integer tradeVersion;

    @Column(name = "TRADE_TYPE")
    @Enumerated(EnumType.STRING)
    TradeType tradeType;

    @Column(name = "STATUS")
    @Enumerated(EnumType.STRING)
    EventMsgProcessStatus status;

    @Column(name = "RECEIVED_DATE_TIME")
    LocalDateTime receivedDateTime;

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CashflowConsumerAuditEntity that)) return false;
        return Objects.equals(id, that.id) && Objects.equals(foCashflowID, that.foCashflowID) && Objects.equals(foCashflowVersion, that.foCashflowVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, foCashflowID, foCashflowVersion);
    }
}
