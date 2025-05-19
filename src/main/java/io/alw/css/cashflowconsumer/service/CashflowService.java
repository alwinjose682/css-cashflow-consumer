package io.alw.css.cashflowconsumer.service;

import io.alw.css.cashflowconsumer.dao.CashflowDao;
import io.alw.css.cashflowconsumer.mapper.FoCashMessageMapper;
import io.alw.css.dbshared.tx.TXRW;
import io.alw.css.domain.cashflow.*;
import io.alw.css.domain.exception.ExceptionConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

import static io.alw.css.cashflowconsumer.service.CashflowVersionService.CFProcessedCheckOutcome.FirstVersion;
import static io.alw.css.cashflowconsumer.service.CashflowVersionService.CFProcessedCheckOutcome.NonFirstVersion;
import static io.alw.css.cashflowconsumer.service.CashflowVersionService.CFProcessedCheckOutcome.AlreadyProcessed;
import static io.alw.css.cashflowconsumer.service.CashflowVersionService.CFProcessedCheckOutcome.LastCashflowIsCancelled;

@Service
public class CashflowService {
    private static final Logger log = LoggerFactory.getLogger(CashflowService.class);
    private final CashflowDao cashflowDao;
    private final CashflowVersionService cashflowVersionService;
    private final CashflowValidator cashflowValidator;
    private final CashflowEnricher cashflowEnricher;
    private final TXRW txrw;

    public CashflowService(CashflowDao cashflowDao, CashflowVersionService cashflowVersionService, CashflowValidator cashflowValidator, CashflowEnricher cashflowEnricher, TXRW txrw) {
        this.cashflowDao = cashflowDao;
        this.cashflowVersionService = cashflowVersionService;
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
        CashflowVersionService.CFProcessedCheckOutcome cfProcessedCheckOutcome = cashflowVersionService.checkAgainstLastProcessedCashflow(foCashflowID, foCashflowVersion, tradeID, tradeVersion);
        switch (cfProcessedCheckOutcome) {
            case FirstVersion _ -> {
                validateAndEnrich(cashflowBuilder);
                Cashflow firstVersionCashflow = cashflowVersionService.createFirstVersionCF(cashflowBuilder, tradeEventType, tradeEventAction, tradeType);
                List<Cashflow> cashflows = List.of(firstVersionCashflow);
            }
            case NonFirstVersion(var lastProcessedCashflow) -> {
                validateAndEnrich(cashflowBuilder);
                List<Cashflow> cashflows = cashflowVersionService.createNonFirstVersionCF(lastProcessedCashflow, cashflowBuilder, tradeEventType, tradeEventAction, tradeType);
            }
            case AlreadyProcessed _ -> {
                log.info("Received duplicate cashflow[foCfID: {}, foCfVer: {}]", foCashflowID, foCashflowVersion);
                return;
            }
            case LastCashflowIsCancelled(var lpcf) -> {
                log.info("Last processed cashflow[cfID: {}, cfVer: {}] is cancelled. No further amendment is permitted. Rejecting current cashflow[foCfID: {}, foCfVer: {}] as {}", lpcf.cashflowID(), lpcf.cashflowVersion(), foCashflowID, foCashflowVersion, ExceptionConstants.ExceptionCategory.UNRECOVERABLE);
                return;
            }
        }
    }

    private void validateAndEnrich(CashflowBuilder cashflowBuilder) {
        cashflowValidator.validatePreEnrichmentData(cashflowBuilder);
        cashflowEnricher.validateAndEnrich(cashflowBuilder);
    }

    private void saveAudit(long foCashflowID, int foCashflowVersion, long tradeID, int tradeVersion, TradeType tradeType) {
        txrw.executeWithoutResult(ts -> cashflowDao.saveCFReceiveAudit(foCashflowID, foCashflowVersion, tradeID, tradeVersion, tradeType));
    }
}
