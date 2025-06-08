package io.alw.css.cashflowconsumer.processor;

import io.alw.css.cashflowconsumer.repository.CashflowStore;
import io.alw.css.cashflowconsumer.processor.rule.RevisionTypeResolver;
import io.alw.css.cashflowconsumer.util.CashflowUtil;
import io.alw.css.dbshared.tx.TXRO;
import io.alw.css.dbshared.tx.TXRW;
import io.alw.css.domain.cashflow.*;
import io.alw.css.domain.exception.CategorizedRuntimeException;
import io.alw.css.domain.exception.ExceptionSubCategory;
import io.alw.css.serialization.cashflow.FoCashMessageAvro;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static io.alw.css.cashflowconsumer.model.constants.ExceptionSubCategoryType.*;

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
public class CashflowVersionManager {
    private final CashflowStore cashflowStore;
    private final TXRW txrw;
    private final TXRO txro;

    public CashflowVersionManager(CashflowStore cashflowStore, TXRW txrw, TXRO txro) {
        this.cashflowStore = cashflowStore;
        this.txrw = txrw;
        this.txro = txro;
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
//    @Transactional(readOnly = true)
    public CFProcessedCheckOutcome checkAgainstLastProcessedCashflow(long foCashflowID, int foCashflowVersion, long tradeID, int tradeVersion) {
//        final Cashflow lastProcessedCashflow = cashflowStore.getLastProcessedCashflow(foCashflowID);
        final Cashflow lastProcessedCashflow = txro.execute(_ -> cashflowStore.getLastProcessedCashflow(foCashflowID));

        if (lastProcessedCashflow == null) { /* if new cashflow */
            return CFProcessedCheckOutcome.FIRST_VERSION;
        } else { /* if not a new cashflow */
            if (isCashflowAlreadyProcessed(foCashflowID, foCashflowVersion, tradeID, tradeVersion, lastProcessedCashflow)) {
                return CFProcessedCheckOutcome.ALREADY_PROCESSED;
            } else {
                if (isCashflowCancelled(lastProcessedCashflow)) {
                    return new CFProcessedCheckOutcome.LastCashflowIsCancelled();
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
//    @Transactional
    public Cashflow createFirstVersionCF(CashflowBuilder cashflowBuilder) {
        long foCashflowVersion = cashflowBuilder.foCashflowVersion();
        RevisionType revisionType = cashflowBuilder.revisionType();
        if (!(foCashflowVersion == CashflowConstants.FO_CASHFLOW_FIRST_VERSION && revisionType == RevisionType.NEW)) {
            throw CategorizedRuntimeException.TECHNICAL_UNRECOVERABLE("Not first version or incorrect revisionType determination", new ExceptionSubCategory(NOT_FIRST_VERSION, null));
        }

//        long cashflowID = cashflowStore.getNewCashflowID();
        long cashflowID = txrw.execute(_ -> cashflowStore.getNewCashflowID());
        int cashflowVersion = CashflowConstants.CSS_CASHFLOW_FIRST_VERSION;
        return cashflowBuilder
                .cashflowID(cashflowID)
                .cashflowVersion(cashflowVersion)
                .latest(true)
                .build();
    }

    /// Creates COR+CAN cashflows or one CAN cashflow depending on [CashflowBuilder#revisionType]
    public List<Cashflow> createNonFirstVersionCF(Cashflow previousCashflow, CashflowBuilder cashflowBuilder) {
        long foCashflowVersion = cashflowBuilder.foCashflowVersion();
        if (foCashflowVersion <= CashflowConstants.FO_CASHFLOW_FIRST_VERSION) {
            throw CategorizedRuntimeException.TECHNICAL_UNRECOVERABLE("Not first version", new ExceptionSubCategory(NOT_FIRST_VERSION, null));
        }

        RevisionType revisionType = cashflowBuilder.revisionType();
        return switch (revisionType) {
            case NEW -> throw CategorizedRuntimeException.TECHNICAL_UNRECOVERABLE("Incorrect RevisionType determination as NEW", new ExceptionSubCategory(INCORRECT_CF_REVISION_TYPE, null));
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
                throw CategorizedRuntimeException.TECHNICAL_UNRECOVERABLE("TradeID does not match tradeID of last processed processed live cashflow", new ExceptionSubCategory(TRADEID_MISMATCH, null));
            } else {//if (lpcfTradeVersion != tradeVersion) {
                throw CategorizedRuntimeException.TECHNICAL_UNRECOVERABLE("TradeVersion is NOT expected to be same when last processed live cashflow's TradeID & ver and CfID & ver matches with the current FO message", new ExceptionSubCategory(TRADEID_MISMATCH, null));
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
    public void computeAndSetRevisionType(CashflowBuilder cashflowBuilder, FoCashMessageAvro foMsg) {
        TradeEventType tradeEventType = FoCashMessageMapper.mapTradeEventType(foMsg);
        TradeEventAction tradeEventAction = FoCashMessageMapper.mapTradeEventAction(foMsg);
        TradeType tradeType = cashflowBuilder.tradeType();
        boolean firstCashflow = CashflowUtil.isFirstFoCashflowVersion(cashflowBuilder.foCashflowVersion());

        Optional<RevisionType> result = RevisionTypeResolver.resolve(firstCashflow, tradeType, tradeEventType, tradeEventAction);
        RevisionType revisionType = result.orElseThrow(() -> CategorizedRuntimeException.TECHNICAL_UNRECOVERABLE("Unable to determine RevisionType from the given combination of inputs", new ExceptionSubCategory(REVISION_TYPE_RESOLUTION_FAILURE, null)));
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
