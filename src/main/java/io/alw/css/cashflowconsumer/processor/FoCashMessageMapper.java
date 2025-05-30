package io.alw.css.cashflowconsumer.processor;

import io.alw.css.domain.cashflow.*;
import io.alw.css.domain.cashflow.CashflowBuilder;
import io.alw.css.domain.common.InputBy;
import io.alw.css.domain.common.PaymentConstants;
import io.alw.css.domain.exception.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Locale;

import static io.alw.css.domain.exception.ExceptionSubCategory.INVALID_FO_VERSION;
import static io.alw.css.domain.exception.ExceptionSubCategory.INVALID_MESSAGE;


/// @see #mapToDomain(FoCashMessage)
public final class FoCashMessageMapper {

    /// Maps below sections of [FoCashMessage] to CSS [Cashflow]
    /// - Fo Cashflow Version Data
    /// - Trade and Cashflow Data
    /// - ObligationData
    ///
    /// Additionally, does the below:
    /// - Creates Cashflow Entry Audit
    /// - For [Cashflow#rate], sets the precision to 10
    /// - For [Cashflow#amount], negates the amount to negative if [FoCashMessage#payOrReceive] is PAY and sets the precision to [PaymentConstants#AMOUNT_SCALE]
    ///
    /// @return CashflowBuilder
    public static CashflowBuilder mapToDomain(FoCashMessage foMsg) {
        CashflowBuilder builder = CashflowBuilder.builder();
        return builder
                // Cashflow Entry Audit
                .inputDateTime(LocalDateTime.now())
                .inputBy(InputBy.CSS_SYS)
                .inputByUserID(InputBy.CSS_SYS.value())

                // Fo Cashflow Version Data
                .foCashflowID(foMsg.cashflowID())
                .foCashflowVersion(doBasicValidationOfFoCashflowVersion(foMsg))
                .tradeID(foMsg.tradeID())
                .tradeVersion(foMsg.tradeVersion())
                // Trade and Cashflow Data
                .tradeType(getTradeType(foMsg))
                .bookCode(foMsg.bookCode())
                .counterBookCode(getCounterBookID(foMsg))
                .secondaryLedgerAccount(foMsg.secondaryLedgerAccount())
                .transactionType(getTransactionType(foMsg))
                .rate(getRate(foMsg))
                .valueDate(foMsg.valueDate())
                .tradeLinks(foMsg.tradeLinks())

                // ObligationData
                .entityCode(foMsg.entityCode())
                .counterpartyCode(foMsg.counterpartyCode())
                .amount(getAmount(foMsg))
                .currCode(foMsg.currCode().toUpperCase(Locale.ROOT));
    }

    private static int doBasicValidationOfFoCashflowVersion(FoCashMessage foMsg) {
        int foCashflowVersion = foMsg.cashflowVersion();
        if (foCashflowVersion < CashflowConstants.FO_CASHFLOW_FIRST_VERSION) {
            throw new CategorizedRuntimeException("fields[foCashflowVersion] is invalid", CssException.TECHNICAL_UNRECOVERABLE(new ExceptionSubCategory(INVALID_FO_VERSION, foMsg)));
        }
        return foCashflowVersion;
    }

    private static BigDecimal getRate(FoCashMessage foMsg) {
        BigDecimal rate = foMsg.rate();
        if (rate != null) {
            rate = rate.setScale(PaymentConstants.RATE_SCALE, RoundingMode.HALF_DOWN);
            return rate;
        } else {
            throw new CategorizedRuntimeException("fields[rate] is null", CssException.TECHNICAL_UNRECOVERABLE(new ExceptionSubCategory(INVALID_MESSAGE, foMsg)));
        }
    }

    private static BigDecimal getAmount(FoCashMessage foMsg) {
        BigDecimal amount = foMsg.amount();
        if (amount != null) {
            amount = amount.setScale(PaymentConstants.AMOUNT_SCALE, RoundingMode.HALF_DOWN);
            return foMsg.payOrReceive() == PayOrReceive.PAY ? amount.negate() : amount;
        } else {
            throw new CategorizedRuntimeException("fields[amount] is null", CssException.TECHNICAL_UNRECOVERABLE(new ExceptionSubCategory(INVALID_MESSAGE, foMsg)));
        }
    }

    private static String getCounterBookID(FoCashMessage foMsg) {
        return foMsg.transactionType() == TransactionType.INTER_BOOK ? foMsg.counterBookCode() : null;
    }

    private static TransactionType getTransactionType(FoCashMessage foMsg) {
        TransactionType transactionType = foMsg.transactionType();
        if (transactionType == null) {
            throw new CategorizedRuntimeException("fields[transactionType] is null", CssException.TECHNICAL_UNRECOVERABLE(new ExceptionSubCategory(INVALID_MESSAGE, foMsg)));
        }
        return foMsg.transactionType();
    }

    private static TradeType getTradeType(FoCashMessage foMsg) {
        TradeType tradeType = foMsg.tradeType();
        if (tradeType == null) {
            throw new CategorizedRuntimeException("fields[tradeType] is null", CssException.TECHNICAL_UNRECOVERABLE(new ExceptionSubCategory(INVALID_MESSAGE, foMsg)));
        }
        return tradeType;
    }
}
