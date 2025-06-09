package io.alw.css.cashflowconsumer.processor.droolsrule;

import io.alw.css.domain.cashflow.TradeEventAction;
import io.alw.css.domain.cashflow.TradeEventType;
import io.alw.css.domain.cashflow.TradeType;
import io.alw.css.domain.cashflow.RevisionType;

import java.util.Optional;

/// RevisionTypeResolver using drools is not used!!
/// Drools is NOT able to locate the rule spreadsheet file from within the jar. But drools does locate the rule spreadsheet file when running the unit tests.
/// Exception is:
/// java.lang.StringIndexOutOfBoundsException: Range [5, 3) out of bounds for length 164
/// at java.base/java.lang.String.checkBoundsBeginEnd(String.java:4950)
/// at java.base/java.lang.String.substring(String.java:2912)
/// at org.drools.ruleunits.impl.RuleUnitProviderImpl.collectResourcesInJar(RuleUnitProviderImpl.java:192)
/// at org.drools.ruleunits.impl.RuleUnitProviderImpl.ruleResourcesForUnitClass(RuleUnitProviderImpl.java:140)
public final class DroolsBasedRevisionTypeResolver {

    /// Fires the drools rules to resolve the [io.alw.css.domain.cashflow.Cashflow#revisionType]
    public static Optional<RevisionType> resolve(boolean firstCashflow, TradeType tradeType, TradeEventType tradeEventType, TradeEventAction tradeEventAction) {
//        RevisionTypeResolverContext context = new RevisionTypeResolverContext(firstCashflow, tradeType, tradeEventType, tradeEventAction);
//        RevisionTypeResolverUnit unit = new RevisionTypeResolverUnit();
//        try (RuleUnitInstance<RevisionTypeResolverUnit> unitInstance = RuleUnitProvider.get().createRuleUnitInstance(unit)) {
//            unit.getContext().add(context);
//            unitInstance.fire();
//            return context.result();
//        }


        throw new RuntimeException("Drools is NOT used! Replaced with custom RevisionType resolution");
    }
}
