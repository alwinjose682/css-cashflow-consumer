package io.alw.css.cashflowconsumer.mapper.rule;

import io.alw.css.domain.cashflow.TradeEventAction;
import io.alw.css.domain.cashflow.TradeEventType;
import io.alw.css.domain.cashflow.TradeType;
import io.alw.css.domain.cashflow.RevisionType;
import org.drools.ruleunits.api.RuleUnitInstance;
import org.drools.ruleunits.api.RuleUnitProvider;

import java.util.Optional;

public final class RevisionTypeResolver {

    /// Fires the drools rules to resolve the [io.alw.css.domain.cashflow.Cashflow#revisionType]
    public static Optional<RevisionType> resolve(boolean firstCashflow, TradeType tradeType, TradeEventType tradeEventType, TradeEventAction tradeEventAction) {
        RevisionTypeResolverContext context = new RevisionTypeResolverContext(firstCashflow, tradeType, tradeEventType, tradeEventAction);
        RevisionTypeResolverUnit unit = new RevisionTypeResolverUnit();
        try (RuleUnitInstance<RevisionTypeResolverUnit> unitInstance = RuleUnitProvider.get().createRuleUnitInstance(unit)) {
            unit.getContext().add(context);
            unitInstance.fire();
            return context.result();
        }
    }
}
