package io.alw.css.cashflowconsumer.processor;

import io.alw.css.cashflowconsumer.model.SsiWithCounterpartyData;
import io.alw.css.cashflowconsumer.model.NostroDetails;
import io.alw.css.cashflowconsumer.model.constants.CashflowConsumerExceptionSubCategoryType;
import io.alw.css.cashflowconsumer.model.properties.SuppressionConfig;
import io.alw.css.cashflowconsumer.service.CacheService;
import io.alw.css.domain.exception.CategorizedRuntimeException;
import io.alw.css.resultapi.*;
import io.alw.css.domain.cashflow.CashflowBuilder;
import io.alw.css.domain.cashflow.TradeType;
import io.alw.css.domain.common.PaymentSuppressionCategory;
import io.alw.css.domain.exception.ExceptionSubCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Map;

import static io.alw.css.domain.cashflow.TransactionType.*;

/// Enriches Cashflow
///
/// @see
public class CashflowEnricher {
    private final static Logger log = LoggerFactory.getLogger(CashflowEnricher.class);
    private final SuppressionConfig suppressionConfig;
    private final CacheService cacheService;

    public CashflowEnricher(SuppressionConfig suppressionConfig, CacheService cacheService) {
        this.suppressionConfig = suppressionConfig;
        this.cacheService = cacheService;
    }

    public void validateAndEnrich(CashflowBuilder builder) {
        String currCode = builder.currCode();
        String counterpartyCode = builder.counterpartyCode();
        TradeType tradeType = builder.tradeType();
        String entityCode = builder.entityCode();

        SsiWithCounterpartyData ssiWithCpData = cacheService.getPrimarySsiWithCounterpartyData(counterpartyCode, currCode, tradeType);
        NostroDetails nostroDetails = cacheService.getNostroDetails(entityCode, currCode, counterpartyCode);

        validateEntityAndCurrCode(builder);
        enrichWithSsiID(builder, ssiWithCpData);
        enrichWithNostroID(builder, nostroDetails);
        setInternalValue(builder, ssiWithCpData);
        setPaymentSuppressionValue(builder);
    }

    public Result<String> validateEntityAndCurrCode(CashflowBuilder builder) {
        String entityCode = builder.entityCode();
        String currCode = builder.currCode();
        boolean entityActive = cacheService.isEntityActive(entityCode);
        boolean currencyActive = cacheService.isCurrencyActive(currCode);
        if (!entityActive) {
            throw CategorizedRuntimeException.BUSINESS_RECOVERABLE("Entity is inactive", new ExceptionSubCategory(CashflowConsumerExceptionSubCategoryType.INACTIVE_ENTITY, null));
        } else if (!currencyActive) {
            throw CategorizedRuntimeException.BUSINESS_RECOVERABLE("Currency is inactive", new ExceptionSubCategory(CashflowConsumerExceptionSubCategoryType.INACTIVE_CURRENCY, null));
        }

        return Result.SUCCESS;
    }

    public void enrichWithNostroID(CashflowBuilder builder, NostroDetails nostroDetails) {
        String entityCode = builder.entityCode();
        String currCode = builder.currCode();

        if (nostroDetails != null) {
            var primaryNostro = nostroDetails.primaryNostro();
            var overridableNostro = nostroDetails.overridableNostro();
            if (overridableNostro != null) {
                log.debug("Overriding primary nostro with secondary nostro configured in counterparty profile. CounterpartyCode: {}, CurrCode: {}, EntityCode: {}", overridableNostro.counterpartyCode(), overridableNostro.currCode(), overridableNostro.entityCode());
                builder.nostroID(overridableNostro.nostroID());
                return;
            } else if (primaryNostro != null) {
                builder.nostroID(primaryNostro.nostroID());
                return;
            }
        }

        var msg = "Nostro is inactive or does not exist. EntityCode: " + entityCode + ", CurrCode: " + currCode;
        throw CategorizedRuntimeException.BUSINESS_RECOVERABLE(msg, new ExceptionSubCategory(CashflowConsumerExceptionSubCategoryType.MISSING_NOSTRO, null));
    }

    public void enrichWithSsiID(CashflowBuilder builder, SsiWithCounterpartyData ssiWithCpData) {
        String counterpartyCode = builder.counterpartyCode();
        String currCode = builder.currCode();
        TradeType tradeType = builder.tradeType();

        if (ssiWithCpData == null) {
            var msg = "Primary SSI or Counterparty is inactive or does not exist. CounterpartyCode: " + counterpartyCode + ", CurrCode: " + currCode + ", TradeType: " + tradeType;
            throw CategorizedRuntimeException.BUSINESS_RECOVERABLE(msg, new ExceptionSubCategory(CashflowConsumerExceptionSubCategoryType.MISSING_SSI, null));
        } else {
            builder.ssiID(ssiWithCpData.ssiId());
        }
    }

    public void setPaymentSuppressionValue(CashflowBuilder builder) {
        String cfCurr = builder.currCode();
        BigDecimal cfAmt = builder.amount();

        if (suppressionConfig.suppressInterbookTX() && builder.transactionType() == INTER_BOOK) {
            builder.paymentSuppressed(true);
            builder.paymentSuppressionCategory(PaymentSuppressionCategory.INTERBOOK);
            return;
        } else if (cfAmt.compareTo(suppressionConfig.highestSuppressibleAmount()) <= 0) {
            for (Map.Entry<String, BigDecimal> entry : suppressionConfig.suppressibleCurrToAmountMap().entrySet()) {
                String curr = entry.getKey();
                BigDecimal amt = entry.getValue();
                if (curr.equalsIgnoreCase(cfCurr) && cfAmt.compareTo(amt) <= 0) {
                    builder.paymentSuppressed(true);
                    builder.paymentSuppressionCategory(PaymentSuppressionCategory.AMOUNT_TOO_SMALL);
                    return;
                }
            }
        }

        builder.paymentSuppressionCategory(PaymentSuppressionCategory.NONE);
    }

    public void setInternalValue(CashflowBuilder builder, SsiWithCounterpartyData ssiWithCpData) {
        boolean internalTransactionType = switch (builder.transactionType()) {
            case INTER_BOOK, INTER_BRANCH, INTER_COMPANY -> true;
            case CLIENT, MARKET, CORPORATE_ACTION -> false;
        };

        boolean internalCounterparty = ssiWithCpData.internal();
        builder.internal(internalTransactionType && internalCounterparty);
    }
}
