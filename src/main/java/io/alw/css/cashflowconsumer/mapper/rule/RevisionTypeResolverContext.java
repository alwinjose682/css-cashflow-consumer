package io.alw.css.cashflowconsumer.mapper.rule;

import io.alw.css.domain.cashflow.TradeEventAction;
import io.alw.css.domain.cashflow.TradeEventType;
import io.alw.css.domain.cashflow.TradeType;
import io.alw.css.domain.cashflow.RevisionType;

import java.util.Optional;

public class RevisionTypeResolverContext {
    //Facts
    private final boolean firstCashflow;
    private final TradeType tradeType;
    private final TradeEventType tradeEventType;
    private final TradeEventAction tradeEventAction;

    //Result
    private RevisionType result;

    public RevisionTypeResolverContext(boolean firstCashflow, TradeType tradeType, TradeEventType tradeEventType, TradeEventAction tradeEventAction) {
        this.firstCashflow = firstCashflow;
        this.tradeType = tradeType;
        this.tradeEventType = tradeEventType;
        this.tradeEventAction = tradeEventAction;
        this.result = null;
    }

    public void setResult(RevisionType result) {
        this.result = result;
    }

    public Optional<RevisionType> result() {
        return Optional.ofNullable(result);
    }

    public boolean firstCashflow() {
        return firstCashflow;
    }

    public TradeType tradeType() {
        return tradeType;
    }

    public TradeEventType tradeEventType() {
        return tradeEventType;
    }

    public TradeEventAction tradeEventAction() {
        return tradeEventAction;
    }
}

