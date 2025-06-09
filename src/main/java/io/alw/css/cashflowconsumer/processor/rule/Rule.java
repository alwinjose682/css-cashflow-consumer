package io.alw.css.cashflowconsumer.processor.rule;

import io.alw.css.domain.cashflow.RevisionType;
import io.alw.css.domain.cashflow.TradeType;

import java.util.List;

public record Rule(
        TradeType tradeType,
        RevisionType result,
        CashflowOrder cashflowOrder,
        List<TradeEventAndAction> tradeEventAndActionRecords) {
}
