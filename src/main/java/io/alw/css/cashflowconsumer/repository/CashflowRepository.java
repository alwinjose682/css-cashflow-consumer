package io.alw.css.cashflowconsumer.repository;

import io.alw.css.cashflowconsumer.model.jpa.CashflowEntity;
import io.alw.css.cashflowconsumer.model.jpa.CashflowEntityPK;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface CashflowRepository extends JpaRepository<CashflowEntity, CashflowEntityPK> {

    @Query(value = """
            select cf from CashflowEntity cf
            where cf.foCashflowID=:foCashflowID
            and cf.foCashflowVersion in (select max(cf1.foCashflowVersion) from CashflowEntity cf1 where cf1.foCashflowID=cf.foCashflowID)
            """)
    CashflowEntity findLastProcessedCashflow(long foCashflowID);
}
