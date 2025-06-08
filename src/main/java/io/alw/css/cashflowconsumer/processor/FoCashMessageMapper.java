package io.alw.css.cashflowconsumer.processor;

import io.alw.css.cashflowconsumer.util.DateUtil;
import io.alw.css.domain.cashflow.*;
import io.alw.css.domain.cashflow.CashflowBuilder;
import io.alw.css.domain.common.InputBy;
import io.alw.css.domain.common.PaymentConstants;
import io.alw.css.domain.exception.*;
import io.alw.css.serialization.cashflow.FoCashMessageAvro;
import io.alw.css.serialization.cashflow.TradeLinkAvro;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

import static io.alw.css.cashflowconsumer.model.constants.ExceptionSubCategoryType.*;

/// @see #mapToDomain(FoCashMessageAvro)
public final class FoCashMessageMapper {
    private final static Logger log = LoggerFactory.getLogger(FoCashMessageMapper.class);

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
    public static CashflowBuilder mapToDomain(FoCashMessageAvro foMsg) {
        CashflowBuilder builder = CashflowBuilder.builder();
        builder
                // Cashflow Entry Audit
                .inputDateTime(LocalDateTime.now())
                .inputBy(InputBy.CSS_SYS)
                .inputByUserID(null)

                // Fo Cashflow Version Data
                .foCashflowID(foMsg.getCashflowID())
                .foCashflowVersion(doBasicValidationOfFoCashflowVersion(foMsg))
                .tradeID(foMsg.getTradeID())
                .tradeVersion(foMsg.getTradeVersion())
                // Trade and Cashflow Data
                .tradeType(mapTradeType(foMsg))
                .bookCode(foMsg.getBookCode())
                .counterBookCode(mapTransactionTypeAndGetCounterBookID(builder, foMsg))
                .secondaryLedgerAccount(foMsg.getSecondaryLedgerAccount())
                .rate(formatRate(foMsg))
                .valueDate(mapValueDate(foMsg))
                .tradeLinks(mapTradeLinks(foMsg))

                // ObligationData
                .entityCode(foMsg.getEntityCode())
                .counterpartyCode(foMsg.getCounterpartyCode())
                .amount(formatAmount(foMsg))
                .currCode(foMsg.getCurrCode().toUpperCase());

        log.debug("Mapped FoCashMessage values to Cashflow. FoCashflowID-Ver: {}-{}", builder.foCashflowID(), builder.foCashflowVersion());
        return builder;
    }

    private static List<TradeLink> mapTradeLinks(FoCashMessageAvro foMsg) {
        List<TradeLinkAvro> tradeLinks = foMsg.getTradeLinks();
        if (tradeLinks != null) {
            return tradeLinks.stream().map(tla -> new TradeLink(tla.getLinkType(), tla.getRelatedReference())).toList();
        } else {
            return null;
        }
    }

    private static LocalDate mapValueDate(FoCashMessageAvro foMsg) {
        String valueDate = foMsg.getValueDate();
        try {
            return DateUtil.formatValueDate(valueDate);
        } catch (DateTimeParseException e) {
            throw CategorizedRuntimeException.TECHNICAL_UNRECOVERABLE("fields[valueDate] is invalid", new ExceptionSubCategory(INVALID_VALUE_DATE, foMsg));
        }
    }

    private static int doBasicValidationOfFoCashflowVersion(FoCashMessageAvro foMsg) {
        int foCashflowVersion = foMsg.getCashflowVersion();
        if (foCashflowVersion < CashflowConstants.FO_CASHFLOW_FIRST_VERSION) {
            throw CategorizedRuntimeException.TECHNICAL_UNRECOVERABLE("fields[foCashflowVersion] is invalid", new ExceptionSubCategory(INVALID_FO_VERSION, foMsg));
        }
        return foCashflowVersion;
    }

    private static BigDecimal formatRate(FoCashMessageAvro foMsg) {
        BigDecimal rate = foMsg.getRate();
        if (rate != null) {
            rate = rate.setScale(PaymentConstants.RATE_SCALE, RoundingMode.HALF_DOWN);
            return rate;
        } else {
            throw CategorizedRuntimeException.TECHNICAL_UNRECOVERABLE("fields[rate] is null", new ExceptionSubCategory(INVALID_RATE, foMsg));
        }
    }

    private static BigDecimal formatAmount(FoCashMessageAvro foMsg) {
        BigDecimal amount = foMsg.getAmount();
        if (amount != null) {
            amount = amount.setScale(PaymentConstants.AMOUNT_SCALE, RoundingMode.HALF_DOWN);
            return PayOrReceive.valueOf(foMsg.getPayOrReceive().name()) == PayOrReceive.PAY ? amount.negate() : amount;
        } else {
            throw CategorizedRuntimeException.TECHNICAL_UNRECOVERABLE("fields[amount] is null", new ExceptionSubCategory(INVALID_AMOUNT, foMsg));
        }
    }

    private static String mapTransactionTypeAndGetCounterBookID(CashflowBuilder builder, FoCashMessageAvro foMsg) {
        if (foMsg.getTransactionType() == null) {
            throw CategorizedRuntimeException.TECHNICAL_UNRECOVERABLE("fields[transactionType] is null", new ExceptionSubCategory(INVALID_MESSAGE, foMsg));
        }

        final TransactionType transactionType;
        try {
            transactionType = TransactionType.valueOf(foMsg.getTransactionType());
            builder.transactionType(transactionType);
        } catch (IllegalArgumentException e) {
            throw CategorizedRuntimeException.TECHNICAL_UNRECOVERABLE("fields[transactionType] is invalid", new ExceptionSubCategory(INVALID_TRANSACTION_TYPE, foMsg));
        }

        return transactionType == TransactionType.INTER_BOOK ? foMsg.getCounterBookCode() : null;
    }

    private static TradeType mapTradeType(FoCashMessageAvro foMsg) {
        String tradeType = foMsg.getTradeType();
        if (tradeType == null) {
            throw CategorizedRuntimeException.TECHNICAL_UNRECOVERABLE("fields[tradeType] is null", new ExceptionSubCategory(INVALID_MESSAGE, foMsg));
        }
        try {
            return TradeType.valueOf(tradeType);
        } catch (IllegalArgumentException e) {
            throw CategorizedRuntimeException.TECHNICAL_UNRECOVERABLE("fields[tradeType] is invalid", new ExceptionSubCategory(INVALID_TRADE_TYPE, foMsg));
        }
    }

    public static TradeEventType mapTradeEventType(FoCashMessageAvro foMsg) {
        String tradeEventType = foMsg.getTradeEventType();
        if (tradeEventType == null) {
            throw CategorizedRuntimeException.TECHNICAL_UNRECOVERABLE("fields[tradeEventType] is null", new ExceptionSubCategory(INVALID_MESSAGE, foMsg));
        }
        try {
            return TradeEventType.valueOf(tradeEventType);
        } catch (IllegalArgumentException e) {
            throw CategorizedRuntimeException.TECHNICAL_UNRECOVERABLE("fields[tradeEventType] is invalid", new ExceptionSubCategory(INVALID_TRADE_EVENT_TYPE, foMsg));
        }
    }

    public static TradeEventAction mapTradeEventAction(FoCashMessageAvro foMsg) {
        io.alw.css.serialization.cashflow.TradeEventAction tradeEventAction = foMsg.getTradeEventAction();
        if (tradeEventAction == null) {
            throw CategorizedRuntimeException.TECHNICAL_UNRECOVERABLE("fields[tradeEventAction] is null", new ExceptionSubCategory(INVALID_MESSAGE, foMsg));
        }
        try {
            return TradeEventAction.valueOf(tradeEventAction.name());
        } catch (IllegalArgumentException e) {
            throw CategorizedRuntimeException.TECHNICAL_UNRECOVERABLE("fields[tradeEventAction] is invalid", new ExceptionSubCategory(INVALID_TRADE_EVENT_ACTION, foMsg));
        }
    }
}
