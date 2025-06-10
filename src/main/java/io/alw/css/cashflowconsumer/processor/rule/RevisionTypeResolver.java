package io.alw.css.cashflowconsumer.processor.rule;

import io.alw.css.domain.cashflow.RevisionType;
import io.alw.css.domain.cashflow.TradeEventAction;
import io.alw.css.domain.cashflow.TradeEventType;
import io.alw.css.domain.cashflow.TradeType;
import io.alw.css.domain.exception.CategorizedRuntimeException;
import io.alw.css.domain.exception.ExceptionSubCategory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.alw.css.cashflowconsumer.model.constants.ExceptionSubCategoryType.REVISION_TYPE_RESOLUTION_FAILURE;

public final class RevisionTypeResolver {
    private static final Map<CashflowOrder, List<Rule>> commonRules = groupByCashflowOrder(CommonRules.rules);
    private static final Map<CashflowOrder, List<Rule>> ndfRules = combineWithCommonRulesAndGroupByCashflowOrder(NdfRules.rules);
    private static final Map<CashflowOrder, List<Rule>> bondRules = combineWithCommonRulesAndGroupByCashflowOrder(BondRules.rules);
    private static final Map<CashflowOrder, List<Rule>> repoRules = combineWithCommonRulesAndGroupByCashflowOrder(RepoRules.rules);
    private static final Map<CashflowOrder, List<Rule>> optionRules = combineWithCommonRulesAndGroupByCashflowOrder(OptionRules.rules);

    public static RevisionType resolve(boolean firstCashflow, TradeType tradeType, TradeEventType tradeEventType, TradeEventAction tradeEventAction) {
        var tradeTypeSpecificRules = getRulesForTradeType(tradeType);
        Rule matchingRule = findMatchingRule(tradeTypeSpecificRules, firstCashflow, tradeEventType, tradeEventAction);
        if (matchingRule == null) {
            throw CategorizedRuntimeException.TECHNICAL_UNRECOVERABLE("Unable to determine RevisionType from the given combination of inputs", new ExceptionSubCategory(REVISION_TYPE_RESOLUTION_FAILURE, null));
        }

        return matchingRule.result();
    }

    private static Rule findMatchingRule(Map<CashflowOrder, List<Rule>> tradeTypeSpecificRules, boolean firstCashflow, TradeEventType tradeEventType, TradeEventAction tradeEventAction) {
        List<Rule> rulesForBothCashflowOrder = tradeTypeSpecificRules.get(CashflowOrder.BOTH);
        Rule matchedRule = findMatchingRule(rulesForBothCashflowOrder, tradeEventType, tradeEventAction);
        if (matchedRule == null) {
            CashflowOrder cashflowOrder = firstCashflow ? CashflowOrder.FIRST : CashflowOrder.NON_FIRST;
            List<Rule> rulesForASpecificCashflowOrder = tradeTypeSpecificRules.get(cashflowOrder);
            return findMatchingRule(rulesForASpecificCashflowOrder, tradeEventType, tradeEventAction);
        } else {
            return matchedRule;
        }
    }

    private static Rule findMatchingRule(List<Rule> rules, TradeEventType tradeEventType, TradeEventAction tradeEventAction) {
        if (rules == null) {
            return null;
        }

        for (Rule rule : rules) {
            for (TradeEventAndAction tea : rule.tradeEventAndActionRecords()) {
                if (tea.event() == tradeEventType && tea.action() == tradeEventAction) {
                    return rule;
                }
            }
        }

        return null;
    }

    private static Map<CashflowOrder, List<Rule>> getRulesForTradeType(TradeType tradeType) {
        return switch (tradeType) {
            case PAYMENT -> commonRules;
            case FX -> commonRules;
            case FX_NDF -> ndfRules;
            case BOND -> bondRules;
            case REPO -> repoRules;
            case OPTION -> optionRules;
        };
    }

    private static Map<CashflowOrder, List<Rule>> combineWithCommonRulesAndGroupByCashflowOrder(List<Rule> rules) {
        var combined = new ArrayList<Rule>();
        combined.addAll(rules);
        combined.addAll(CommonRules.rules);
        return groupByCashflowOrder(combined);
    }

    private static Map<CashflowOrder, List<Rule>> groupByCashflowOrder(List<Rule> rules) {
        return rules.stream().collect(Collectors.groupingBy(Rule::cashflowOrder));
    }
}
