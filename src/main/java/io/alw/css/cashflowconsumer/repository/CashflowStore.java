package io.alw.css.cashflowconsumer.repository;

import io.alw.css.cashflowconsumer.model.constants.ExceptionSubCategoryType;
import io.alw.css.cashflowconsumer.model.jpa.*;
import io.alw.css.cashflowconsumer.repository.mapper.CashflowMapper;
import io.alw.css.domain.cashflow.Cashflow;
import io.alw.css.domain.cashflow.RevisionType;
import io.alw.css.domain.exception.CategorizedRuntimeException;
import io.alw.css.domain.exception.ExceptionSubCategory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public final class CashflowStore {
    private final static Logger log = LoggerFactory.getLogger(CashflowStore.class);

    @PersistenceContext
    private EntityManager em;
    private final CashflowRepository cashflowRepository;
    private final CashflowRejectionRepository cashflowRejectionRepository;

    public CashflowStore(CashflowRepository cashflowRepository, CashflowRejectionRepository cashflowRejectionRepository) {
        this.cashflowRepository = cashflowRepository;
        this.cashflowRejectionRepository = cashflowRejectionRepository;
    }

    /// **TODO**: When switching to Oracle DB, check whether Hibernate still returns Long
    ///
    /// **SUBTLE ISSUE**:
    ///
    /// When below property is set, the nextval of the sequence returned by Hibernate is of type java.lang.Long. When this property is not set, Hibernate returns java.math.BigDecimal
    /// - property: spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.OracleDialect
    /// - properties 'spring.jpa.database-platform' and 'spring.jpa.properties.hibernate.dialect' are for the same purpose and have the same effect
    ///
    /// Exception:
    /// class java.lang.Long cannot be cast to class java.math.BigDecimal (java.lang.Long and java.math.BigDecimal are in module java.base of loader 'bootstrap')
    ///
    /// **UPDATE**: Hibernate version 6.X has changed the mapping of DB type to Java type. (Looks like this applies ONLY for native queries)
    /// Check: https://discourse.hibernate.org/t/oracledialect-changes-in-number-type-mappings-in-version-6/7503
    /// Still, this difference in mapping happen only when enabling the said property!
    /// NOTE: JDBC still maps NUMBER to BigDecimal
    ///
    /// Example: Hibernate 6.X maps Oracle NUMBER type based on its width to java.lang.Integer, Long, BigDecimal etc. instead of the behaviour of old Hibernate versions that used to map oracle NUMBER to BigDecimal. Float types are mapped differently
    public long getNewCashflowID() {
//        return ((BigDecimal) em.createNativeQuery("select CSS.cashflow_seq.nextval from dual").getSingleResult()).longValue();
        return (long) em.createNativeQuery("select CSS.cashflow_seq.nextval from dual").getSingleResult();
    }

    /// This method returns null if no result. Does not use Optional
    public Cashflow getLastProcessedCashflow(long foCashflowID) {
        CashflowEntity lpcf = cashflowRepository.findLastProcessedCashflow(foCashflowID);
        if (lpcf != null) {
            return CashflowMapper.instance().mapToDomain_excludingAssociations(lpcf);
        } else {
            return null;
        }
    }

    public void saveRejection(CashflowRejectionEntity cfr) {
        cashflowRejectionRepository.save(cfr);
    }

    public void saveFirstVersionCF(Cashflow cf) {
        log.trace("Saving Cashflow[{}-{}] to DB. tradeLinks: {}", cf.cashflowID(), cf.cashflowVersion(), cf.tradeLinks() != null);
        CashflowEntity cfe = CashflowMapper.mapToEntity(cf);
        cashflowRepository.save(cfe);
    }

    /// This method does following actions atomically:
    /// 1. Update last processed cashflow's 'latest' field to 'N'
    /// 2. If exactly ONE row is updated in step 1, continues to step 3. If zero rows updated, throws a [io.alw.css.domain.exception.CategorizedRuntimeException]
    /// 3. inserts the offset and correction cashflows to DB. (Correction cashflow is created with latest='Y')
    /// 4. return
    ///
    /// NOTE: Since this method does multiple individual updates, it may be better to use a database procedure instead
    public void saveNonFirstVersionCF(Map<RevisionType, Cashflow> cashflows, Cashflow lastProcessedCashflow) {
        long lpcfId = lastProcessedCashflow.cashflowID();
        int lpcfVer = lastProcessedCashflow.cashflowVersion();

        // Step 1: Update last processed cashflow's 'latest' field to 'N'
        int numOfRowsUpdated = cashflowRepository.updateLastProcessedCashflowToNonLatest(lpcfId, lpcfVer);

        // Step 2 and 3: If exactly ONE row is updated, persists the cashflows. Otherwise, throws an exception
        if (numOfRowsUpdated == 1) {
            cashflows.values().stream()
                    .peek(cf -> log.trace("Saving Cashflow[{}-{}] to DB. tradeLinks: {}", cf.cashflowID(), cf.cashflowVersion(), cf.tradeLinks() != null))
                    .map(CashflowMapper::mapToEntity)
                    .forEach(cashflowRepository::save);
        } else if (numOfRowsUpdated == 0) {
            Cashflow amendCf = cashflows.get(RevisionType.COR);
            String errMsg = "Unable to persist cashflow amendment[foCfID: " + amendCf.foCashflowID() + ", foCfVer: " + amendCf.foCashflowVersion() + "] to database."
                    + " LastProcessedCashflow[cfID: " + lpcfId + ", cfVer: " + lpcfVer + "] was updated possibly by a concurrent transaction";
            log.error(errMsg);
            throw CategorizedRuntimeException.TECHNICAL_RECOVERABLE(errMsg, new ExceptionSubCategory(ExceptionSubCategoryType.CF_PERSISTENCE_FAILURE, lastProcessedCashflow)
            );
        } else if (numOfRowsUpdated > 1) {
            Cashflow amendCf = cashflows.get(RevisionType.COR);
            String errMsg = "Unable to persist cashflow amendment[foCfID: " + amendCf.foCashflowID() + ", foCfVer: " + amendCf.foCashflowVersion() + "] to database."
                    + ". Multiple cashflows exist in database with latest='Y' for CashflowID " + lpcfId + ". The cashflow is in invalid state and this should NOT happen.";
            log.error(errMsg);
            throw CategorizedRuntimeException.TECHNICAL_RECOVERABLE(errMsg, new ExceptionSubCategory(ExceptionSubCategoryType.CF_PERSISTENCE_FAILURE, lastProcessedCashflow)
            );
        }
    }
}
