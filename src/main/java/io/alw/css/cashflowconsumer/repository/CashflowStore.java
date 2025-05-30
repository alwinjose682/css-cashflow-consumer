package io.alw.css.cashflowconsumer.repository;

import io.alw.css.cashflowconsumer.model.jpa.CashflowConsumerAuditEntity;
import io.alw.css.cashflowconsumer.model.jpa.CashflowEntity;
import io.alw.css.cashflowconsumer.repository.mapper.CashflowEntityMapper;
import io.alw.css.domain.cashflow.Cashflow;
import io.alw.css.domain.cashflow.TradeType;
import io.alw.css.domain.common.EventMsgProcessStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.time.LocalDateTime;

public final class CashflowStore {
    @PersistenceContext
    private EntityManager em;
    private final CashflowRepository cashflowRepository;

    public CashflowStore(CashflowRepository cashflowRepository) {
        this.cashflowRepository = cashflowRepository;
    }


    public long getNewCashflowID() {
        return (long) em.createNativeQuery("select CSS_SERVICES.cashflow_seq.nextval from dual").getSingleResult();
    }

    private long getCFConsumerAuditId() {
        return (long) em.createNativeQuery("select CSS_SERVICES.cf_consumer_audit_seq.nextval from dual").getSingleResult();
    }

    public void saveCFReceiveAudit(long foCashflowID, int foCashflowVersion, long tradeID, int tradeVersion, TradeType tradeType) {
        long id = getCFConsumerAuditId();
        CashflowConsumerAuditEntity cashflowConsumerAuditEntity = new CashflowConsumerAuditEntity(id, foCashflowID, foCashflowVersion, tradeID, tradeVersion, tradeType, EventMsgProcessStatus.RECEIVED, LocalDateTime.now());
        em.persist(cashflowConsumerAuditEntity);
    }

    /// This method returns null if no result. Does not use Optional
    public Cashflow getLastProcessedCashflow(long foCashflowID) {
        CashflowEntity lpcf = cashflowRepository.findLastProcessedCashflow(foCashflowID);
        if (lpcf != null) {
            return CashflowEntityMapper.instance().mapToDomain(lpcf);
        } else {
            return null;
        }
    }

}
