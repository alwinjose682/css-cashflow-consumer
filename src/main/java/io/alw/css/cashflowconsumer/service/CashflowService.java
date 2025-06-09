package io.alw.css.cashflowconsumer.service;

import io.alw.css.cashflowconsumer.model.constants.ExceptionSubCategoryType;
import io.alw.css.cashflowconsumer.model.jpa.CashflowRejectionEntity;
import io.alw.css.cashflowconsumer.processor.CFProcessedCheckOutcome;
import io.alw.css.cashflowconsumer.processor.CashflowEnricher;
import io.alw.css.cashflowconsumer.processor.CashflowVersionManager;
import io.alw.css.cashflowconsumer.repository.CashflowStore;
import io.alw.css.cashflowconsumer.processor.FoCashMessageMapper;
import io.alw.css.cashflowconsumer.util.DateUtil;
import io.alw.css.dbshared.tx.TXRW;
import io.alw.css.domain.cashflow.*;
import io.alw.css.domain.common.InputBy;
import io.alw.css.domain.common.YesNo;
import io.alw.css.domain.exception.CategorizedRuntimeException;
import io.alw.css.domain.exception.ExceptionCategory;
import io.alw.css.domain.exception.ExceptionType;
import io.alw.css.serialization.cashflow.FoCashMessageAvro;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

import static io.alw.css.cashflowconsumer.processor.CFProcessedCheckOutcome.*;

@Service
public class CashflowService {
    private static final Logger log = LoggerFactory.getLogger(CashflowService.class);
    private final CashflowStore cashflowStore;
    private final CashflowVersionManager cashflowVersionManager;
    private final CashflowEnricher cashflowEnricher;
    private final TXRW txrw;

    public CashflowService(CashflowStore cashflowStore, CashflowVersionManager cashflowVersionManager, CashflowEnricher cashflowEnricher, TXRW txrw) {
        this.cashflowStore = cashflowStore;
        this.cashflowVersionManager = cashflowVersionManager;
        this.cashflowEnricher = cashflowEnricher;
        this.txrw = txrw;
    }

    public void process(FoCashMessageAvro foMsg, InputBy inputBy) {
        long foCashflowID = foMsg.getCashflowID();
        int foCashflowVersion = foMsg.getCashflowVersion();
        long tradeID = foMsg.getTradeID();
        int tradeVersion = foMsg.getTradeVersion();

        log.info("Received FoCashMessage[foCashflowID: {}, foCashflowVersion: {}, tradeID: {}, tradeVersion: {}]", foCashflowID, foCashflowVersion, tradeID, tradeVersion);
        try {
            CashflowBuilder cashflowBuilder = FoCashMessageMapper.mapToDomain(foMsg);
            cashflowVersionManager.computeAndSetRevisionType(cashflowBuilder, foMsg);
            CFProcessedCheckOutcome outcome = cashflowVersionManager.checkAgainstLastProcessedCashflow(foCashflowID, foCashflowVersion, tradeID, tradeVersion);
            evaluateAndProcessFurther(outcome, cashflowBuilder, foMsg);
        } catch (CategorizedRuntimeException e) {
            log.info("Failed to process cashflow. FoCashflowID-Ver: {}-{}. Msg: {}", foCashflowID, foCashflowVersion, e.getMessage(), e);
            rejectCashflow(foMsg, e, inputBy);
        } catch (Exception e) {
            log.info("Failed to process cashflow. FoCashflowID-Ver: {}-{}. Msg: {}", foCashflowID, foCashflowVersion, e.getMessage(), e);
            rejectCashflow(foMsg, CategorizedRuntimeException.UNKNOWN(e.getMessage(), foMsg), inputBy);
        }
    }

    private void evaluateAndProcessFurther(CFProcessedCheckOutcome outcome, CashflowBuilder cashflowBuilder, FoCashMessageAvro foMsg) {
        long foCashflowID = foMsg.getCashflowID();
        int foCashflowVersion = foMsg.getCashflowVersion();

        switch (outcome) {
            case FirstVersion _ -> {
                cashflowEnricher.validateAndEnrich(cashflowBuilder);
                Cashflow cf = cashflowVersionManager.createFirstVersionCF(cashflowBuilder);
                txrw.executeWithoutResult(_ -> cashflowStore.saveFirstVersionCF(cf));
                log.info("Successfully processed cashflow. CashflowID-Ver: {}-{}", cf.cashflowID(), cf.cashflowVersion());
            }
            case NonFirstVersion(var lastProcessedCashflow) -> {
                cashflowEnricher.validateAndEnrich(cashflowBuilder);
                Map<RevisionType, Cashflow> cashflows = cashflowVersionManager.createNonFirstVersionCF(lastProcessedCashflow, cashflowBuilder);
                txrw.executeWithoutResult(_ -> cashflowStore.saveNonFirstVersionCF(cashflows, lastProcessedCashflow));
                logSuccessfulProcessingOfNonFirstVersion(cashflows);
            }
            case AlreadyProcessed _ -> {
                log.info("Received duplicate cashflow[foCfID: {}, foCfVer: {}]", foCashflowID, foCashflowVersion);
            }
            case LastCashflowIsCancelled _ -> {
                ExceptionType exceptionType = ExceptionType.BUSINESS;
                ExceptionCategory exceptionCategory = ExceptionCategory.UNRECOVERABLE;
                String exceptionSubCategory = ExceptionSubCategoryType.LAST_CASHFLOW_IS_CANCELLED;
                String msg = "No further amendment is permitted when last cashflow is cancelled";
                boolean replayable = false;
                int numOfRetries = 0;
                LocalDateTime createdDateTime = LocalDateTime.now();
                InputBy inputBy = InputBy.CSS_SYS;

                log.info("Last cashflow is cancelled. No further amendment is permitted. FoCashflowID-Ver: {}-{}", foCashflowID, foCashflowVersion);
                rejectCashflow(foMsg, exceptionType, exceptionCategory, exceptionSubCategory, msg, replayable, numOfRetries, createdDateTime, inputBy);
            }
        }
    }

    private void logSuccessfulProcessingOfNonFirstVersion(Map<RevisionType, Cashflow> cashflows) {
        final Cashflow cf = cashflows.get(RevisionType.COR);
        if (cf != null) {
            log.info("Successfully processed cashflow. CashflowID-Ver: {}-{}", cf.cashflowID(), cf.cashflowVersion());
        } else {
            final Cashflow cf1 = cashflows.get(RevisionType.CAN);
            log.info("Successfully processed cashflow. CashflowID-Ver: {}-{}", cf1.cashflowID(), cf1.cashflowVersion());
        }
    }

    private void rejectCashflow(FoCashMessageAvro foMsg, CategorizedRuntimeException cre, InputBy inputBy) {
        rejectCashflow(foMsg, cre.type(), cre.category(), cre.subCategory().type(), cre.msg(), cre.replayable(), cre.numOfRetries(), cre.createdTime(), inputBy);
    }

    private void rejectCashflow(FoCashMessageAvro foMsg, ExceptionType exceptionType, ExceptionCategory exceptionCategory, String exceptionSubCategory, String msg, boolean replayable, int numOfRetries, LocalDateTime createdDateTime, InputBy inputBy) {
        long foCashflowID = foMsg.getCashflowID();
        int foCashflowVersion = foMsg.getCashflowVersion();
        try {
            CashflowRejectionEntity cfr = new CashflowRejectionEntity();
            cfr
                    .setFoCashflowID(foCashflowID)
                    .setFoCashflowVersion(foCashflowVersion)
                    .setTradeID(foMsg.getTradeID())
                    .setTradeVersion(foMsg.getTradeVersion())
                    .setTradeType(foMsg.getTradeType())
                    .setValueDate(foMsg.getValueDate() == null ? null : DateUtil.formatValueDate(foMsg.getValueDate()))
                    .setEntityCode(foMsg.getEntityCode())
                    .setCounterpartyCode(foMsg.getCounterpartyCode())
                    .setAmount(foMsg.getAmount())
                    .setCurrCode(foMsg.getCurrCode())
                    .setExceptionType(exceptionType.name())
                    .setExceptionCategory(exceptionCategory.name())
                    .setExceptionSubCategory(exceptionSubCategory)
                    .setMsg(msg)
                    .setReplayable(replayable ? YesNo.Y : YesNo.N)
                    .setNumOfRetries(numOfRetries)
                    .setCreatedDateTime(createdDateTime)
                    .setInputBy(inputBy)
                    .setUpdatedDateTime(LocalDateTime.now())
            ;

            txrw.executeWithoutResult(_ -> cashflowStore.saveRejection(cfr));
        } catch (Exception e) {
            log.error("Failed to save cashflow rejection to database. FoCashflowID-Ver: {}-{}", foCashflowID, foCashflowVersion, e);
            throw new RuntimeException(e);
        }
    }
}
