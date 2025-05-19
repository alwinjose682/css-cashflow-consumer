package io.alw.css.cashflowconsumer.service;

import io.alw.css.cashflowconsumer.model.properties.SuppressionConfig;
import io.alw.css.domain.cashflow.CashflowBuilder;
import io.alw.css.domain.common.PaymentSuppressionCategory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

import static io.alw.css.domain.cashflow.TransactionType.*;

/// Enriches Cashflow
///
/// @see
@Service
public class CashflowEnricher {
    private final SuppressionConfig suppressionConfig;

    public CashflowEnricher(SuppressionConfig suppressionConfig) {
        this.suppressionConfig = suppressionConfig;
    }

    public void validateAndEnrich(CashflowBuilder builder) {
        setInternalValue(builder);
        setPaymentSuppressionValue(builder);
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
        boolean internal = switch (builder.transactionType()) {
            case INTER_BOOK, INTER_BRANCH, INTER_COMPANY -> true;
            case CLIENT, MARKET, CORPORATE_ACTION -> false;
        };

        builder.internal(internal);
    }

}
