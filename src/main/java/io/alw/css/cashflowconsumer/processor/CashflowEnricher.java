package io.alw.css.cashflowconsumer.processor;

import io.alw.css.cashflowconsumer.model.SsiWithCounterpartyData;
import io.alw.css.cashflowconsumer.model.NostroDetails;
import io.alw.css.cashflowconsumer.model.constants.ExceptionSubCategoryType;
import io.alw.css.cashflowconsumer.model.properties.SuppressionConfig;
import io.alw.css.cashflowconsumer.service.CacheService;
import io.alw.css.domain.exception.CategorizedRuntimeException;
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

        log.debug("Successfully Validated and Enriched the cashflow. FoCashflowID-Ver: {}-{}", builder.foCashflowID(), builder.foCashflowVersion());
    }

    public void validateEntityAndCurrCode(CashflowBuilder builder) {
        String entityCode = builder.entityCode();
        String currCode = builder.currCode();
        boolean entityActive = cacheService.isEntityActive(entityCode);
        boolean currencyActive = cacheService.isCurrencyActive(currCode);
        if (!entityActive) {
            throw CategorizedRuntimeException.BUSINESS_RECOVERABLE("Entity is inactive", new ExceptionSubCategory(ExceptionSubCategoryType.INACTIVE_ENTITY, null));
        } else if (!currencyActive) {
            throw CategorizedRuntimeException.BUSINESS_RECOVERABLE("Currency is inactive", new ExceptionSubCategory(ExceptionSubCategoryType.INACTIVE_CURRENCY, null));
        }
    }

    public void enrichWithNostroID(CashflowBuilder builder, NostroDetails nostroDetails) {
        String entityCode = builder.entityCode();
        String currCode = builder.currCode();

        if (nostroDetails != null) {
            var primaryNostro = nostroDetails.primaryNostro();
            var overridableNostro = nostroDetails.overridableNostro();
            if (overridableNostro != null) {
                String nostroID = overridableNostro.nostroID();
                builder.nostroID(nostroID);
                log.info("Enriched NostroID[{}] by overriding primary nostro with secondary configured in counterparty profile. CounterpartyCode: {}, CurrCode: {}, EntityCode: {}, FoCashflowID-Ver: {}-{}", nostroID, overridableNostro.counterpartyCode(), overridableNostro.currCode(), overridableNostro.entityCode(), builder.foCashflowID(), builder.foCashflowVersion());
                return;
            } else if (primaryNostro != null) {
                String nostroID = primaryNostro.nostroID();
                builder.nostroID(nostroID);
                log.debug("Enriched with nostroID: {}. FoCashflowID-Ver: {}-{}", nostroID, builder.foCashflowID(), builder.foCashflowVersion());
                return;
            }
        }

        var msg = "Nostro is inactive or does not exist. EntityCode: " + entityCode + ", CurrCode: " + currCode;
        throw CategorizedRuntimeException.BUSINESS_RECOVERABLE(msg, new ExceptionSubCategory(ExceptionSubCategoryType.MISSING_NOSTRO, null));
    }

    public void enrichWithSsiID(CashflowBuilder builder, SsiWithCounterpartyData ssiWithCpData) {
        String counterpartyCode = builder.counterpartyCode();
        String currCode = builder.currCode();
        TradeType tradeType = builder.tradeType();

        if (ssiWithCpData == null) {
            var msg = "Primary SSI or Counterparty is inactive or does not exist. CounterpartyCode: " + counterpartyCode + ", CurrCode: " + currCode + ", TradeType: " + tradeType;
            throw CategorizedRuntimeException.BUSINESS_RECOVERABLE(msg, new ExceptionSubCategory(ExceptionSubCategoryType.MISSING_SSI, null));
        } else {
            String ssiID = ssiWithCpData.ssiId();
            builder.ssiID(ssiID);
            log.debug("Enriched with ssiID: {}. FoCashflowID-Ver: {}-{}", ssiID, builder.foCashflowID(), builder.foCashflowVersion());
        }
    }

    public void setPaymentSuppressionValue(CashflowBuilder builder) {
        String cfCurr = builder.currCode();
        BigDecimal cfAmt = builder.amount();

        if (suppressionConfig.suppressInterbookTX() && builder.transactionType() == INTER_BOOK) {
            builder.paymentSuppressionCategory(PaymentSuppressionCategory.INTERBOOK);
            log.debug("Cashflow is categorized to be suppressed. SuppressionCategory: {}, FoCashflowID-Ver: {}-{}", PaymentSuppressionCategory.INTERBOOK, builder.foCashflowID(), builder.foCashflowVersion());
            return;
        } else if (cfAmt.compareTo(suppressionConfig.highestSuppressibleAmount()) <= 0) {
            for (Map.Entry<String, BigDecimal> entry : suppressionConfig.suppressibleCurrToAmountMap().entrySet()) {
                String curr = entry.getKey();
                BigDecimal amt = entry.getValue();
                if (curr.equalsIgnoreCase(cfCurr) && cfAmt.compareTo(amt) <= 0) {
                    builder.paymentSuppressionCategory(PaymentSuppressionCategory.AMOUNT_TOO_SMALL);
                    log.debug("Cashflow is categorized to be suppressed. SuppressionCategory: {}, FoCashflowID-Ver: {}-{}", PaymentSuppressionCategory.AMOUNT_TOO_SMALL, builder.foCashflowID(), builder.foCashflowVersion());
                    return;
                }
            }
        }

        builder.paymentSuppressionCategory(PaymentSuppressionCategory.NONE);
        log.debug("Cashflow is NOT suppressible due to any reason. FoCashflowID-Ver: {}-{}", builder.foCashflowID(), builder.foCashflowVersion());
    }

    public void setInternalValue(CashflowBuilder builder, SsiWithCounterpartyData ssiWithCpData) {
        boolean internalTransactionType = switch (builder.transactionType()) {
            case INTER_BOOK, INTER_BRANCH, INTER_COMPANY -> true;
            case CLIENT, MARKET, CORPORATE_ACTION -> false;
        };

        boolean internalCounterparty = ssiWithCpData.internal();
        builder.internal(internalTransactionType && internalCounterparty);
        log.debug("Set internal/external value for the cashflow. FoCashflowID-Ver: {}-{}", builder.foCashflowID(), builder.foCashflowVersion());
    }
}
