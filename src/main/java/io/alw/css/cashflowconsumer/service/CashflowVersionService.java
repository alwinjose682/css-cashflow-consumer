package io.alw.css.cashflowconsumer.service;

import io.alw.css.cashflowconsumer.dao.CashflowDao;
import io.alw.css.cashflowconsumer.mapper.rule.RevisionTypeResolver;
import io.alw.css.cashflowconsumer.util.CashflowUtil;
import io.alw.css.dbshared.tx.TXRO;
import io.alw.css.dbshared.tx.TXRW;
import io.alw.css.domain.cashflow.*;
import io.alw.css.domain.exception.CategorizedRuntimeException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static io.alw.css.domain.exception.ExceptionConstants.ExceptionCategory.UNRECOVERABLE;
import static io.alw.css.domain.exception.ExceptionConstants.ExceptionSubCategory.*;
import static io.alw.css.domain.exception.ExceptionConstants.ExceptionType.TECHNICAL;

/// Does the following:
/// - Determines whether the new cashflow is firstVersion, nonFirstVersion or alreadyProcessed
/// - computes the CSS [RevisionType] based on FO's [TradeEventAction], [TradeEventType] and [TradeType]
/// - sets the appropriate [Cashflow#latest] value
/// - creates NEW, COR+Offsetting, CAN cashflows according to above outcome.
/// - assigns new cashflowID and version for the cashflow to be processed
/// - Finally, creates the [Cashflow]
///
/// Q: In an amendment(COR) or cancellation(CAN) scenario, how to identify which cashflow the COR or CAN cashflow offsets or vice versa?
/// - There is no extra identifier to determine this.
/// It is ensured that the CFs are processed sequentially(and it must be so) based on the [io.alw.css.domain.cashflow.FoCashMessage#cashflowVersion]
/// in order to guarantee that an N+2 version of a CF does not offset a live Nth version.
/// Due to this property, the CF the COR or CAN cashflow offsets can be identified simply based on the version number(which is straight forward).
@Service
public class CashflowVersionService {
    private final CashflowDao cashflowDao;
    private final TXRW txrw;
    private final TXRO txro;

    public CashflowVersionService(CashflowDao cashflowDao, TXRW txrw, TXRO txro) {
        this.cashflowDao = cashflowDao;
        this.txrw = txrw;
        this.txro = txro;
    }

    public sealed interface CFProcessedCheckOutcome {
        AlreadyProcessed ALREADY_PROCESSED = new AlreadyProcessed();
        FirstVersion FIRST_VERSION = new FirstVersion();

        record AlreadyProcessed() implements CFProcessedCheckOutcome {
        }

        record FirstVersion() implements CFProcessedCheckOutcome {
        }

        record NonFirstVersion(Cashflow lastProcessedCashflow) implements CFProcessedCheckOutcome {
        }

        record LastCashflowIsCancelled(Cashflow lastProcessedCashflow) implements CFProcessedCheckOutcome {
        }
    }

    /// Checks if the cashflow is [CFProcessedCheckOutcome.FirstVersion] or [CFProcessedCheckOutcome.NonFirstVersion] or [CFProcessedCheckOutcome.AlreadyProcessed]
    ///
    /// This is determined based on the last processed live cashflow. 'Last processed cashflow' can occur as a result of one of the below:
    /// 1. A COR or CAN from FO
    /// 2. A COR or CAN by CSS User
    ///
    /// If last processed cashflow is cancelled, then no further amendment is allowed. An exception is thrown in that case.
    /// Once a cashflow is cancelled, a new cashflowID needs to be used by FO for the same trade.
    ///
    /// @return CFProcessedCheckOutcome
    @Transactional(readOnly = true)
    public CFProcessedCheckOutcome checkAgainstLastProcessedCashflow(long foCashflowID, int foCashflowVersion, long tradeID, int tradeVersion) {
        Optional<Cashflow> cashflow = cashflowDao.getLastProcessedCashflow(foCashflowID, foCashflowVersion);
//        Optional<Cashflow> cashflow = txro.execute(_ -> cashflowDao.getLastProcessedCashflow(foCashflowID, foCashflowVersion));

        if (cashflow.isEmpty()) { /* if new cashflow */
            return CFProcessedCheckOutcome.FIRST_VERSION;
        } else { /* if not a new cashflow */
            Cashflow lastProcessedCashflow = cashflow.get();
            if (isCashflowAlreadyProcessed(foCashflowID, foCashflowVersion, tradeID, tradeVersion, lastProcessedCashflow)) {
                return CFProcessedCheckOutcome.ALREADY_PROCESSED;
            } else {
                if (isCashflowCancelled(lastProcessedCashflow)) {
                    return new CFProcessedCheckOutcome.LastCashflowIsCancelled(lastProcessedCashflow);
                }
                return new CFProcessedCheckOutcome.NonFirstVersion(lastProcessedCashflow);
            }
        }
    }

    /// Creates NEW cashflow. A NEW cashflow is possible only once, for the first version. Once a cashflow is cancelled, a new cashflowID needs to be used by FO for the same trade.
    /// Below values are explicitly set
    /// - cashflowID = new cashflowID
    /// - cashflowVersion = [CashflowConstants#CSS_CASHFLOW_FIRST_VERSION]
    /// - latest = true
    ///
    /// @implNote This method calls a dao method that does select on a DB sequence. Hence this method must be called in RW transaction(which is so currently)
    // TODO: When transaction is readOnly for JpaTransactionManager, does spring cause libs to acquire a RO physical connection or just optimizes JPA dirty checking etc? AskVlad
    @Transactional
    public Cashflow createFirstVersionCF(CashflowBuilder cashflowBuilder, TradeEventType tradeEventType, TradeEventAction tradeEventAction, TradeType tradeType) {
        computeAndSetRevisionType(cashflowBuilder, tradeEventType, tradeEventAction, tradeType);

        long foCashflowVersion = cashflowBuilder.foCashflowVersion();
        RevisionType revisionType = cashflowBuilder.revisionType();
        if (!(foCashflowVersion == CashflowConstants.FO_CASHFLOW_FIRST_VERSION && revisionType == RevisionType.NEW)) {
            throw new CategorizedRuntimeException(TECHNICAL, UNRECOVERABLE, NOT_FIRST_VERSION, "Not first version or incorrect revisionType determination");
        }

        long cashflowID = cashflowDao.getNewCashflowID();
//        long cashflowID = (long) txrw.execute(_ -> cashflowDao.getNewCashflowID());
        int cashflowVersion = CashflowConstants.CSS_CASHFLOW_FIRST_VERSION;
        return cashflowBuilder
                .cashflowID(cashflowID)
                .cashflowVersion(cashflowVersion)
                .latest(true)
                .build();
    }

    /// Creates COR+CAN cashflows or one CAN cashflow depending on [CashflowBuilder#revisionType]
    List<Cashflow> createNonFirstVersionCF(Cashflow previousCashflow, CashflowBuilder cashflowBuilder, TradeEventType tradeEventType, TradeEventAction tradeEventAction, TradeType tradeType) {
        computeAndSetRevisionType(cashflowBuilder, tradeEventType, tradeEventAction, tradeType);

        long foCashflowVersion = cashflowBuilder.foCashflowVersion();
        if (foCashflowVersion <= CashflowConstants.FO_CASHFLOW_FIRST_VERSION) {
            throw new CategorizedRuntimeException(TECHNICAL, UNRECOVERABLE, NOT_FIRST_VERSION, "Not first version");
        }

        RevisionType revisionType = cashflowBuilder.revisionType();
        return switch (revisionType) {
            case NEW -> throw new CategorizedRuntimeException(TECHNICAL, UNRECOVERABLE, INCORRECT_CF_REVISION_TYPE, "RevisionType is incorrectly determined to be NEW when not a new cashflow");
            case COR -> createAmendCFAndOffsetForPrevCF(previousCashflow, cashflowBuilder);
            case CAN -> {
                Cashflow cancelCashflow = createCancelCF(previousCashflow, cashflowBuilder);
                yield List.of(cancelCashflow);
            }
        };
    }

    private boolean isCashflowAlreadyProcessed(long foCashflowID, int foCashflowVersion, long tradeID, int tradeVersion, Cashflow lastProcessedCashflow) {
        long lpcfFoCashflowID = lastProcessedCashflow.foCashflowID();
        int lpcfFoCashflowVersion = lastProcessedCashflow.foCashflowVersion();
        long lpcfCashflowTradeID = lastProcessedCashflow.tradeID();
        int lpcfTradeVersion = lastProcessedCashflow.tradeVersion();

        if (foCashflowID == lpcfFoCashflowID && foCashflowVersion == lpcfFoCashflowVersion) {
            if (lpcfCashflowTradeID == tradeID && lpcfTradeVersion == tradeVersion) {
                return true;
            } else if (lpcfCashflowTradeID != tradeID) { // It is valid to have same tradeID but different tradeVersion
                throw new CategorizedRuntimeException(TECHNICAL, UNRECOVERABLE, TRADEID_MISMATCH, "TradeID does not match tradeID of last processed processed live cashflow");
            } else {//if (lpcfTradeVersion != tradeVersion) {
                throw new CategorizedRuntimeException(TECHNICAL, UNRECOVERABLE, TRADEID_MISMATCH, "TradeVersion is NOT expected to be same when last processed live cashflow's TradeID & ver and CfID & ver matches with the current FO message");
            }
        } else {
            return false;
        }
    }

    private boolean isCashflowCancelled(Cashflow cashflow) {
        return cashflow.revisionType() == RevisionType.CAN;
    }

    /// Determines the appropriate [Cashflow#revisionType] based on:
    /// - [FoCashMessage#tradeType],
    /// - [FoCashMessage#tradeEventType],
    /// - [FoCashMessage#tradeEventAction]
    /// - [FoCashMessage#cashflowVersion]
    private static void computeAndSetRevisionType(CashflowBuilder cashflowBuilder, TradeEventType tradeEventType, TradeEventAction tradeEventAction, TradeType tradeType) {
        boolean firstCashflow = CashflowUtil.isFirstFoCashflowVersion(cashflowBuilder.foCashflowVersion());
        if (tradeType == null || tradeEventType == null || tradeEventAction == null) {
            throw new CategorizedRuntimeException(TECHNICAL, UNRECOVERABLE, INVALID_MESSAGE, "fields[tradeType,tradeEventType,tradeEventAction] are null");
        }
        Optional<RevisionType> result = RevisionTypeResolver.resolve(firstCashflow, tradeType, tradeEventType, tradeEventAction);
        RevisionType revisionType = result.orElseThrow(() -> new CategorizedRuntimeException(TECHNICAL, UNRECOVERABLE, INVALID_MESSAGE, "Unable to determine RevisionType from the given combination of inputs"));
        cashflowBuilder.revisionType(revisionType);
    }

    /// Creates a CAN cashflow from the previous cashflow which must be live. This offsets the live cashflow.
    /// Offsetting cashflow has all fields of `prevCashflow` as is except modified values for below fields:
    /// - revisionType = CAN
    /// - cashflowID = `currentCashflowBuilder`.cashflowID
    /// - cashflowVersion = `currentCashflowBuilder`.cashflowVersion + 1
    /// - foCashflowID = `currentCashflowBuilder`.foCashflowID
    /// - foCashflowVersion = `currentCashflowBuilder`.foCashflowVersion
    /// - amount = -1 * `prevCashflow`.amount (CF does not have bsn direction)
    /// - latest = true
    /// - foInputDateTime = `currentCashflowBuilder`.foInputDateTime
    /// - inputDateTime = `currentCashflow`.inputDateTime
    private Cashflow createCancelCF(Cashflow previousCashflow, CashflowBuilder currentCashflowBuilder) {
        BigDecimal prevCashflowAmount = previousCashflow.amount();
        return CashflowBuilder.builder(previousCashflow)
                .revisionType(RevisionType.CAN)
                .cashflowID(currentCashflowBuilder.cashflowID())
                .cashflowVersion(currentCashflowBuilder.cashflowVersion() + 1)
                .foCashflowID(currentCashflowBuilder.foCashflowID())
                .foCashflowVersion(currentCashflowBuilder.foCashflowVersion())
                .amount(prevCashflowAmount.negate())
                .latest(true)
                .inputDateTime(currentCashflowBuilder.inputDateTime())
                .build();
    }

    private List<Cashflow> createAmendCFAndOffsetForPrevCF(Cashflow previousCashflow, CashflowBuilder cashflowBuilder) {
        Cashflow offsetCashflow = createOffsetCashflow(previousCashflow, cashflowBuilder);
        Cashflow amendCashflow = createAmendCashflow(previousCashflow, cashflowBuilder, offsetCashflow);
        return List.of(offsetCashflow, amendCashflow);
    }

    ///  Creates current cashflow with:
    /// - `cashflowID` = `prevCashflow`.cashflowID
    /// - `cashflowVersion` = `offsetCashflow`.cashflowVersion + 1
    /// - latest = true
    private Cashflow createAmendCashflow(Cashflow previousCashflow, CashflowBuilder cashflowBuilder, Cashflow offsetCashflow) {
        return cashflowBuilder
                .cashflowID(previousCashflow.cashflowID())
                .cashflowVersion(offsetCashflow.cashflowVersion() + 1)
                .latest(true)
                .build();
    }

    /// Creates a CAN cashflow to offset the last processed cashflow.
    /// Offsetting cashflow has all fields of `prevCashflow` as is except modified values for below fields:
    /// - revisionType = CAN
    /// - cashflowVersion = `previousCashflow`.cashflowVersion() + 1
    /// - amount = -1 * `prevCashflow`.amount (CF does not have bsn direction)
    /// - latest = false
    /// - inputDateTime = `currCashflowBuilder`.inputDateTime
    private Cashflow createOffsetCashflow(Cashflow previousCashflow, CashflowBuilder currCashflowBuilder) {
        BigDecimal prevCashflowAmount = previousCashflow.amount();
        return CashflowBuilder.builder(previousCashflow)
                .revisionType(RevisionType.CAN)
                .cashflowVersion(previousCashflow.cashflowVersion() + 1)
                .amount(prevCashflowAmount.negate())
                .latest(false)
                .inputDateTime(currCashflowBuilder.inputDateTime())
                .build();
    }
}
