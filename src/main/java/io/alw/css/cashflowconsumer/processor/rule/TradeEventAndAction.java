package io.alw.css.cashflowconsumer.processor.rule;

import io.alw.css.domain.cashflow.TradeEventAction;
import io.alw.css.domain.cashflow.TradeEventType;

record TradeEventAndAction(TradeEventType event, TradeEventAction action) {
}
