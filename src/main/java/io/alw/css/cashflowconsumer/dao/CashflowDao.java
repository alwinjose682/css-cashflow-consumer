package io.alw.css.cashflowconsumer.dao;

import io.alw.css.cashflowconsumer.model.jpa.CashflowConsumerAuditEntity;
import io.alw.css.domain.cashflow.Cashflow;
import io.alw.css.domain.cashflow.TradeType;
import io.alw.css.domain.common.EventMsgProcessStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
public final class CashflowDao {
    @PersistenceContext
    private EntityManager em;

    public long getNewCashflowID() {
        return (long) em.createNativeQuery("select CSS_SERVICES.cashflow_seq.nextval from dual").getSingleResult();
    }

    private long getCFConsumerAuditId() {
        return (long) em.createNativeQuery("select CSS_SERVICES.cf_consumer_audit_seq.nextval from dual").getSingleResult();
    }

    public void saveCFReceiveAudit(long foCashflowID, int foCashflowVersion, long tradedID, int tradeVersion, TradeType tradeType) {
        long id = getCFConsumerAuditId();
        CashflowConsumerAuditEntity cashflowConsumerAuditEntity = new CashflowConsumerAuditEntity(id, foCashflowID, foCashflowVersion, tradedID, tradeVersion, tradeType, EventMsgProcessStatus.RECEIVED, LocalDateTime.now());
        em.persist(cashflowConsumerAuditEntity);
    }

    public Optional<Cashflow> getLastProcessedCashflow(long foCashflowID, int foCashflowVersion) {
        return null;
    }

}
