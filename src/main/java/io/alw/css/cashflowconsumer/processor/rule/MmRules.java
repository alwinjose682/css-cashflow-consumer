package io.alw.css.cashflowconsumer.processor.rule;

import io.alw.css.domain.cashflow.TradeEventType;

import java.util.List;

import static io.alw.css.cashflowconsumer.processor.rule.CashflowOrder.NON_FIRST;
import static io.alw.css.domain.cashflow.RevisionType.COR;
import static io.alw.css.domain.cashflow.TradeEventAction.ADD;
import static io.alw.css.domain.cashflow.TradeType.REPO;

public final class MmRules implements RuleDefinition {
    private final static Rule rule1 = new Rule(REPO, COR, NON_FIRST, List.of(
            new TradeEventAndAction(TradeEventType.ROLL, ADD),
            new TradeEventAndAction(TradeEventType.TERMINATE, ADD)
    ));

    static List<Rule> rules = List.of(rule1);
}
