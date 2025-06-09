package io.alw.css.cashflowconsumer.processor.rule;

import io.alw.css.domain.cashflow.TradeEventType;

import java.util.List;

import static io.alw.css.cashflowconsumer.processor.rule.CashflowOrder.BOTH;
import static io.alw.css.cashflowconsumer.processor.rule.CashflowOrder.NON_FIRST;
import static io.alw.css.domain.cashflow.RevisionType.CAN;
import static io.alw.css.domain.cashflow.RevisionType.NEW;
import static io.alw.css.domain.cashflow.TradeEventAction.ADD;
import static io.alw.css.domain.cashflow.TradeType.OPTION;

public final class OptionRules implements RuleDefinition {
    private final static Rule rule1 = new Rule(OPTION, NEW, BOTH, List.of(
            new TradeEventAndAction(TradeEventType.EXERCISE, ADD)
    ));

    private final static Rule rule2 = new Rule(OPTION, CAN, NON_FIRST, List.of(
            new TradeEventAndAction(TradeEventType.KNOCK_OUT, ADD),
            new TradeEventAndAction(TradeEventType.EXPIRE, ADD)
    ));

    static List<Rule> rules = List.of(rule1,rule2);
}
