package io.alw.css.cashflowconsumer.repository;

import io.alw.css.cashflowconsumer.model.jpa.CashflowRejectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CashflowRejectionRepository extends JpaRepository<CashflowRejectionEntity, Long> {
}
