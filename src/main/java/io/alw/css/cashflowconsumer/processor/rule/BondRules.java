package io.alw.css.cashflowconsumer.processor.rule;

import io.alw.css.domain.cashflow.TradeEventType;

import java.util.List;

import static io.alw.css.cashflowconsumer.processor.rule.CashflowOrder.BOTH;
import static io.alw.css.domain.cashflow.RevisionType.NEW;
import static io.alw.css.domain.cashflow.TradeEventAction.ADD;
import static io.alw.css.domain.cashflow.TradeType.BOND;

public final class BondRules implements RuleDefinition {
    private final static Rule rule1 = new Rule(BOND, NEW, BOTH, List.of(
            new TradeEventAndAction(TradeEventType.INTEREST_ACTION, ADD),
            new TradeEventAndAction(TradeEventType.MATURE, ADD)
    ));
    static List<Rule> rules = List.of(rule1);
}
