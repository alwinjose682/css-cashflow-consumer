package io.alw.css.cashflowconsumer.processor;

import io.alw.css.cashflowconsumer.model.CounterpartyAndSsiDetails;
import io.alw.css.cashflowconsumer.model.NostroDetails;
import io.alw.css.cashflowconsumer.model.constants.CashflowConsumerExceptionSubCategoryType;
import io.alw.css.cashflowconsumer.model.properties.SuppressionConfig;
import io.alw.css.cashflowconsumer.service.CacheService;
import io.alw.css.commonlib.*;
import io.alw.css.domain.cashflow.CashflowBuilder;
import io.alw.css.domain.cashflow.TradeType;
import io.alw.css.domain.common.PaymentSuppressionCategory;
import io.alw.css.domain.exception.CssException;
import io.alw.css.domain.exception.ExceptionSubCategory;

import java.math.BigDecimal;
import java.util.Map;

import static io.alw.css.domain.cashflow.TransactionType.*;

/// Enriches Cashflow
///
/// @see
public class CashflowEnricher {
    private final SuppressionConfig suppressionConfig;
    private final CacheService cacheService;

    public CashflowEnricher(SuppressionConfig suppressionConfig, CacheService cacheService) {
        this.suppressionConfig = suppressionConfig;
        this.cacheService = cacheService;
    }

    public Result<?> validateAndEnrich(CashflowBuilder builder) {
        String currCode = builder.currCode();
        String counterpartyCode = builder.counterpartyCode();
        TradeType tradeType = builder.tradeType();
        String entityCode = builder.entityCode();

        CounterpartyAndSsiDetails cpAndSsiDetails = cacheService.getCounterpartyAndSsiDetails(counterpartyCode, currCode, tradeType, true);
        NostroDetails nostroDetails = cacheService.getNostroDetails(entityCode, currCode, counterpartyCode);

        return Result.of(() -> enrichWithSsiID(builder, cpAndSsiDetails, counterpartyCode, currCode, tradeType))
                .andThen(() -> enrichWithNostroID(nostroDetails))
                .accept(_ -> setInternalValue(builder))
                .accept(_ -> setPaymentSuppressionValue(builder));
    }

    private Result<?> enrichWithNostroID(NostroDetails nostroDetails) {

    }

    private Result<?> enrichWithSsiID(CashflowBuilder builder, CounterpartyAndSsiDetails cpAndSsiDetails, String counterpartyCode, String currCode, TradeType tradeType) {
//        return new Success();

        return switch (cpAndSsiDetails) {
            case null -> {
                var msg = "No primary SSI found for Counterparty: " + counterpartyCode + ", Currency: " + currCode + ", TradeType: " + tradeType;
                yield new Failure(CssException.BUSINESS_RECOVERABLE(msg, new ExceptionSubCategory(CashflowConsumerExceptionSubCategoryType.MISSING_SSI, cpAndSsiDetails)));
            }
            case CounterpartyAndSsiDetails sd when !sd.activeCounterparty() -> {
                var msg = "Counterparty: " + counterpartyCode + " is inactive";
                yield new Failure(CssException.BUSINESS_RECOVERABLE(msg, new ExceptionSubCategory(CashflowConsumerExceptionSubCategoryType.INACTIVE_COUNTERPARTY, cpAndSsiDetails)));
            }
            case CounterpartyAndSsiDetails sd when !sd.activeSsi() -> {
                var msg = "Primary SSI: " + sd.ssiId() + "[ssVer: " + sd.ssiVersion() + "] is inactive";
                yield new Failure(CssException.BUSINESS_RECOVERABLE(msg, new ExceptionSubCategory(CashflowConsumerExceptionSubCategoryType.INACTIVE_COUNTERPARTY, cpAndSsiDetails)));
            }

            case CounterpartyAndSsiDetails sd -> {
                builder.ssiID(sd.ssiId());
                yield new Success<>("Success");
            }
        };
    }

    private void setPaymentSuppressionValue(CashflowBuilder builder) {
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

        builder.paymentSuppressed(false);
        builder.paymentSuppressionCategory(null);
    }

    private void setInternalValue(CashflowBuilder builder) {
//        TODO: Change this internal trade identification logic to base upon counterpartyCode is internal
        boolean internalTransactionType = switch (builder.transactionType()) {
            case INTER_BOOK, INTER_BRANCH, INTER_COMPANY -> true;
            case CLIENT, MARKET, CORPORATE_ACTION -> false;
        };

        boolean internalCounterparty = cacheService.;

        builder.internal(internalTransactionType && internalCounterparty);
    }

}
