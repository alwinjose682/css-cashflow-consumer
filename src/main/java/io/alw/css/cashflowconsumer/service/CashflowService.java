package io.alw.css.cashflowconsumer.service;

import io.alw.css.cashflowconsumer.processor.CashflowEnricher;
import io.alw.css.cashflowconsumer.processor.CashflowValidator;
import io.alw.css.cashflowconsumer.processor.CashflowVersionManager;
import io.alw.css.cashflowconsumer.repository.CashflowStore;
import io.alw.css.cashflowconsumer.processor.FoCashMessageMapper;
import io.alw.css.dbshared.tx.TXRW;
import io.alw.css.domain.cashflow.*;
import io.alw.css.domain.exception.CssException;
import io.alw.css.domain.exception.ExceptionCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

import static io.alw.css.cashflowconsumer.processor.CashflowVersionManager.CFProcessedCheckOutcome.FirstVersion;
import static io.alw.css.cashflowconsumer.processor.CashflowVersionManager.CFProcessedCheckOutcome.NonFirstVersion;
import static io.alw.css.cashflowconsumer.processor.CashflowVersionManager.CFProcessedCheckOutcome.AlreadyProcessed;
import static io.alw.css.cashflowconsumer.processor.CashflowVersionManager.CFProcessedCheckOutcome.LastCashflowIsCancelled;

@Service
public class CashflowService {
    private static final Logger log = LoggerFactory.getLogger(CashflowService.class);
    private final CashflowStore cashflowStore;
    private final CashflowVersionManager cashflowVersionManager;
    private final CashflowValidator cashflowValidator;
    private final CashflowEnricher cashflowEnricher;
    private final TXRW txrw;

    public CashflowService(CashflowStore cashflowStore, CashflowVersionManager cashflowVersionManager, CashflowValidator cashflowValidator, CashflowEnricher cashflowEnricher, TXRW txrw) {
        this.cashflowStore = cashflowStore;
        this.cashflowVersionManager = cashflowVersionManager;
        this.cashflowValidator = cashflowValidator;
        this.cashflowEnricher = cashflowEnricher;
        this.txrw = txrw;
    }

    //TODO: New field introduced tradeVersion. Need to include this field in the verifications done in checkAgainstLastProcessedCashflow etc.
    public void process(FoCashMessage foMsg) {
        long foCashflowID = foMsg.cashflowID();
        int foCashflowVersion = foMsg.cashflowVersion();
        long tradeID = foMsg.tradeID();
        int tradeVersion = foMsg.tradeVersion();
        TradeType tradeType = foMsg.tradeType();
        TradeEventType tradeEventType = foMsg.tradeEventType();
        TradeEventAction tradeEventAction = foMsg.tradeEventAction();

        saveAudit(foCashflowID, foCashflowVersion, tradeID, tradeVersion, tradeType);
        CashflowBuilder cashflowBuilder = FoCashMessageMapper.mapToDomain(foMsg);
        CashflowVersionManager.CFProcessedCheckOutcome cfProcessedCheckOutcome = cashflowVersionManager.checkAgainstLastProcessedCashflow(foCashflowID, foCashflowVersion, tradeID, tradeVersion);
        switch (cfProcessedCheckOutcome) {
            case FirstVersion _ -> {
                validateAndEnrich(cashflowBuilder);
                Cashflow firstVersionCashflow = cashflowVersionManager.createFirstVersionCF(cashflowBuilder, tradeEventType, tradeEventAction, tradeType);
                List<Cashflow> cashflows = List.of(firstVersionCashflow);
            }
            case NonFirstVersion(var lastProcessedCashflow) -> {
                validateAndEnrich(cashflowBuilder);
                List<Cashflow> cashflows = cashflowVersionManager.createNonFirstVersionCF(lastProcessedCashflow, cashflowBuilder, tradeEventType, tradeEventAction, tradeType);
            }
            case AlreadyProcessed _ -> {
                log.info("Received duplicate cashflow[foCfID: {}, foCfVer: {}]", foCashflowID, foCashflowVersion);
                return;
            }
            case LastCashflowIsCancelled(var lpcf) -> {
                log.info("Last processed cashflow[cfID: {}, cfVer: {}] is cancelled. No further amendment is permitted. Rejecting cashflow[foCfID: {}, foCfVer: {}] as {}", lpcf.cashflowID(), lpcf.cashflowVersion(), foCashflowID, foCashflowVersion, ExceptionCategory.UNRECOVERABLE);
                return;
            }
        }
    }

    private void validateAndEnrich(CashflowBuilder cashflowBuilder) {
        cashflowValidator.validatePreEnrichmentData(cashflowBuilder);
        cashflowEnricher.validateAndEnrich(cashflowBuilder);
    }

    private void saveAudit(long foCashflowID, int foCashflowVersion, long tradeID, int tradeVersion, TradeType tradeType) {
        txrw.executeWithoutResult(ts -> cashflowStore.saveCFReceiveAudit(foCashflowID, foCashflowVersion, tradeID, tradeVersion, tradeType));
    }
}
