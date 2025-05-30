package io.alw.css.cashflowconsumer.processor.rule;

import io.alw.css.domain.cashflow.*;

import org.drools.ruleunits.api.RuleUnitInstance;
import org.drools.ruleunits.api.RuleUnitProvider;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static io.alw.css.cashflowconsumer.processor.rule.RevisionTypeResolverTest.CashflowOrder.*;
import static io.alw.css.domain.cashflow.TradeType.*;
import static io.alw.css.domain.cashflow.RevisionType.*;
import static io.alw.css.domain.cashflow.TradeEventAction.*;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RevisionTypeResolverTest {
    record TradeEventAndAction(TradeEventType event, TradeEventAction action) {
    }

    enum CashflowOrder {
        FIRST, NON_FIRST, BOTH
    }

    record Rule(TradeType tradeType, RevisionType result, CashflowOrder cashflowOrder, List<TradeEventAndAction> tradeEventAndActionRecords) {
    }

    interface AnyTradeRule {
        Rule rule1 = new Rule(FX, NEW, FIRST, List.of(
                new TradeEventAndAction(TradeEventType.NEW_TRADE, ADD),
                new TradeEventAndAction(TradeEventType.REBOOK, ADD),
                new TradeEventAndAction(TradeEventType.AMEND, ADD),
                new TradeEventAndAction(TradeEventType.AMEND, MODIFY),
                new TradeEventAndAction(TradeEventType.CORRECTION, ADD),
                new TradeEventAndAction(TradeEventType.CORRECTION, MODIFY)
        ));
        Rule rule2 = new Rule(FX, COR, NON_FIRST, List.of(
                new TradeEventAndAction(TradeEventType.AMEND, ADD),
                new TradeEventAndAction(TradeEventType.AMEND, MODIFY),
                new TradeEventAndAction(TradeEventType.AMEND, REMOVE),
                new TradeEventAndAction(TradeEventType.BOOK_MOVE, ADD),
                new TradeEventAndAction(TradeEventType.BOOK_MOVE, MODIFY),
                new TradeEventAndAction(TradeEventType.CORRECTION, ADD),
                new TradeEventAndAction(TradeEventType.CORRECTION, MODIFY),
                new TradeEventAndAction(TradeEventType.CORRECTION, REMOVE)
        ));
        Rule rule3 = new Rule(FX, CAN, NON_FIRST, List.of(
                new TradeEventAndAction(TradeEventType.CANCEL, ADD)
        ));
    }

    interface OptionRule {

        Rule rule1 = new Rule(OPTION, NEW, BOTH, List.of(
                new TradeEventAndAction(TradeEventType.EXERCISE, ADD)
        ));

        Rule rule2 = new Rule(OPTION, CAN, NON_FIRST, List.of(
                new TradeEventAndAction(TradeEventType.KNOCK_OUT, ADD),
                new TradeEventAndAction(TradeEventType.EXPIRE, ADD)
        ));
    }

    interface RepoRule {
        Rule rule1 = new Rule(REPO, COR, NON_FIRST, List.of(
                new TradeEventAndAction(TradeEventType.ROLL, ADD),
                new TradeEventAndAction(TradeEventType.TERMINATE, ADD)
        ));
    }

    interface NdfRule {

        Rule rule1 = new Rule(FX_NDF, NEW, BOTH, List.of(
                new TradeEventAndAction(TradeEventType.FIX, ADD)
        ));

        Rule rule2 = new Rule(FX_NDF, CAN, NON_FIRST, List.of(
                new TradeEventAndAction(TradeEventType.UN_FIX, ADD)
        ));
    }

    interface BondRule {
        Rule rule1 = new Rule(BOND, NEW, BOTH, List.of(
                new TradeEventAndAction(TradeEventType.INTEREST_ACTION, ADD),
                new TradeEventAndAction(TradeEventType.MATURE, ADD)
        ));
    }


    @Test
    @Order(1)
    void testAnyTradeRules() {
        List.of(
                AnyTradeRule.rule1,
                AnyTradeRule.rule2,
                AnyTradeRule.rule3
        ).forEach(this::fireRule);
    }

    @Test
    @Order(2)
    void testOptionRules() {
        List.of(
                OptionRule.rule1,
                OptionRule.rule2
        ).forEach(this::fireRule);
    }

    @Test
    @Order(4)
    void testRepoRules() {
        List.of(
                RepoRule.rule1
        ).forEach(this::fireRule);
    }

    @Test
    @Order(5)
    void testNdfRules() {
        List.of(
                NdfRule.rule1,
                NdfRule.rule2
        ).forEach(this::fireRule);
    }

    @Test
    @Order(6)
    void testBondRules() {
        List.of(
                BondRule.rule1
        ).forEach(this::fireRule);
    }

    @Test
    @Order(3)
    void ruleEvaluationWithoutDrools() {

        Instant start = Instant.now();
        record RuleContext(TradeType tradeType, CashflowOrder cashflowOrder, TradeEventType tradeEventType, TradeEventAction tradeEventAction) {
        }
        RuleContext rc1 = new RuleContext(FX, NON_FIRST, TradeEventType.AMEND, MODIFY);

        RevisionType[] result = new RevisionType[1];
        Stream.of(
                        AnyTradeRule.rule1,
                        AnyTradeRule.rule2,
                        AnyTradeRule.rule3,
                        OptionRule.rule1,
                        OptionRule.rule2,
                        RepoRule.rule1,
                        NdfRule.rule1,
                        NdfRule.rule2,
                        BondRule.rule1)
                .forEach(rule -> {
                    TradeType tradeType = rc1.tradeType();
                    CashflowOrder cashflowOrder = rc1.cashflowOrder();
                    TradeEventType tradeEventType = rc1.tradeEventType();
                    TradeEventAction tradeEventAction = rc1.tradeEventAction();

                    if (tradeType.equals(rule.tradeType) && cashflowOrder == rule.cashflowOrder()) {
                        for (TradeEventAndAction tea : rule.tradeEventAndActionRecords()) {
                            if (tea.event() == tradeEventType && tea.action() == tradeEventAction) {
                                result[0] = rule.result;
                            }
                        }
                    }
                });


        System.out.println("Result=" + result[0] + ". A single rule evaluated without drools in " + Duration.between(start, Instant.now()));
    }

    void fireRule(Rule rule) {
        Instant start = Instant.now();

        TradeType tradeType = rule.tradeType();
        List<TradeEventAndAction> tradeEventAndActions = rule.tradeEventAndActionRecords();
        RevisionType expectedResult = rule.result();
        List<Boolean> firstCashflow = switch (rule.cashflowOrder()) {
            case FIRST -> List.of(true);
            case NON_FIRST -> List.of(false);
            case BOTH -> List.of(true, false);
        };

        firstCashflow
                .forEach(firstCf -> tradeEventAndActions
                        .forEach(tea -> {
                            var context = new RevisionTypeResolverContext(firstCf, tradeType, tea.event(), tea.action());
                            RevisionTypeResolverUnit unit = new RevisionTypeResolverUnit();
                            try (RuleUnitInstance<RevisionTypeResolverUnit> unitInstance = RuleUnitProvider.get().createRuleUnitInstance(unit)) {
                                unit.getContext().add(context);
                                unitInstance.fire();
                                Optional<RevisionType> result = context.result();
                                result.ifPresentOrElse(r -> assertEquals(expectedResult, r), () -> fail("RevisionType is null"));
                            }
                        }));

        System.out.println("Drools evaluated in " + Duration.between(start, Instant.now()));
    }
}
