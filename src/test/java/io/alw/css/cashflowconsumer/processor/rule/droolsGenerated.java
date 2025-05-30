//package io.alw.css.cashflowconsumer.domain.mapper.rule;
//
//import org.drools.modelcompiler.dsl.pattern.D;
//import org.drools.model.Index.ConstraintType;
//import io.alw.css.domain.cashflow.FoTradeEventAction;
//import io.alw.css.domain.cashflow.RevisionType;
//import io.alw.css.domain.cashflow.FoTradeEventType;
//import static io.alw.css.cashflowconsumer.domain.mapper.rule.RulesC16CF0A4C1450DD639D8585C47D62814.*;
//import static io.alw.css.cashflowconsumer.domain.mapper.rule.RulesC16CF0A4C1450DD639D8585C47D62814_RevisionTypeResolverUnit.*;
//
//public class RulesC16CF0A4C1450DD639D8585C47D62814_RevisionTypeResolverUnitRuleMethods0 {
//
//    /**
//     * Rule name: RTRule_13
//     */
//    public static org.drools.model.Rule rule_RTRule__13() {
//        final org.drools.model.Variable<io.alw.css.cashflowconsumer.domain.mapper.rule.RevisionTypeResolverContext> var_$c = D.declarationOf(io.alw.css.cashflowconsumer.domain.mapper.rule.RevisionTypeResolverContext.class,
//                DomainClassesMetadataC16CF0A4C1450DD639D8585C47D62814.io_alw_css_cashflowconsumer_domain_mapper_rule_RevisionTypeResolverContext_Metadata_INSTANCE,
//                "$c",
//                D.entryPoint("context"));
//        org.drools.model.Rule rule = D.rule("io.alw.css.cashflowconsumer.domain.mapper.rule",
//                        "RTRule_13")
//                .unit(io.alw.css.cashflowconsumer.domain.mapper.rule.RevisionTypeResolverUnit.class)
//                .build(D.pattern(var_$c).expr("GENERATED_24CAD371EA6D05B2F2323FF42676082A",
//                                io.alw.css.cashflowconsumer.domain.mapper.rule.PB5.LambdaPredicateB5D608D27BF0D14E77F55C2C8CF78DD1.INSTANCE,
//                                D.alphaIndexedBy(boolean.class,
//                                        org.drools.model.Index.ConstraintType.EQUAL,
//                                        DomainClassesMetadataC16CF0A4C1450DD639D8585C47D62814.io_alw_css_cashflowconsumer_domain_mapper_rule_RevisionTypeResolverContext_Metadata_INSTANCE.getPropertyIndex("firstCashflow"),
//                                        io.alw.css.cashflowconsumer.domain.mapper.rule.P10.LambdaExtractor103D9ECB55CEAD46A1896882D7948C23.INSTANCE,
//                                        true)).expr("GENERATED_CF08F0754F701098E6E3E995E1F0D32E",
//                                io.alw.css.cashflowconsumer.domain.mapper.rule.P47.LambdaPredicate474CD5CD7E5067104D74E3EC0E35D2A3.INSTANCE,
//                                D.alphaIndexedBy(io.alw.css.domain.cashflow.FoTradeEventAction.class,
//                                        org.drools.model.Index.ConstraintType.EQUAL,
//                                        DomainClassesMetadataC16CF0A4C1450DD639D8585C47D62814.io_alw_css_cashflowconsumer_domain_mapper_rule_RevisionTypeResolverContext_Metadata_INSTANCE.getPropertyIndex("tradeEventAction"),
//                                        io.alw.css.cashflowconsumer.domain.mapper.rule.P99.LambdaExtractor996B964FEE005ABD60C242A8734D168E.INSTANCE,
//                                        "FoTradeEventAction.ADD")).expr("GENERATED_1FED41ABFE7D69720748D1883E8823B7",
//                                io.alw.css.cashflowconsumer.domain.mapper.rule.PC5.LambdaPredicateC51E0EB93C93305275A115D4453C3EB4.INSTANCE,
//                                D.alphaIndexedBy(io.alw.css.domain.cashflow.FoTradeEventType.class,
//                                        org.drools.model.Index.ConstraintType.EQUAL,
//                                        DomainClassesMetadataC16CF0A4C1450DD639D8585C47D62814.io_alw_css_cashflowconsumer_domain_mapper_rule_RevisionTypeResolverContext_Metadata_INSTANCE.getPropertyIndex("tradeEventType"),
//                                        io.alw.css.cashflowconsumer.domain.mapper.rule.P2E.LambdaExtractor2ECAA204DFA39888AC434DAAEB518C39.INSTANCE,
//                                        "FoTradeEventType.NEW_TRADE")),
//                        D.on(var_$c).execute(io.alw.css.cashflowconsumer.domain.mapper.rule.P3E.LambdaConsequence3EFAD8A70ECFE2CCC497AABB609B0897.INSTANCE));
//        return rule;
//    }
//
//    /**
//     * Rule name: RTRule_14
//     */
//    public static org.drools.model.Rule rule_RTRule__14() {
//        final org.drools.model.Variable<io.alw.css.cashflowconsumer.domain.mapper.rule.RevisionTypeResolverContext> var_$c = D.declarationOf(io.alw.css.cashflowconsumer.domain.mapper.rule.RevisionTypeResolverContext.class,
//                DomainClassesMetadataC16CF0A4C1450DD639D8585C47D62814.io_alw_css_cashflowconsumer_domain_mapper_rule_RevisionTypeResolverContext_Metadata_INSTANCE,
//                "$c",
//                D.entryPoint("context"));
//        org.drools.model.Rule rule = D.rule("io.alw.css.cashflowconsumer.domain.mapper.rule",
//                        "RTRule_14")
//                .unit(io.alw.css.cashflowconsumer.domain.mapper.rule.RevisionTypeResolverUnit.class)
//                .build(D.pattern(var_$c).expr("GENERATED_CF08F0754F701098E6E3E995E1F0D32E",
//                                io.alw.css.cashflowconsumer.domain.mapper.rule.P47.LambdaPredicate474CD5CD7E5067104D74E3EC0E35D2A3.INSTANCE,
//                                D.alphaIndexedBy(io.alw.css.domain.cashflow.FoTradeEventAction.class,
//                                        org.drools.model.Index.ConstraintType.EQUAL,
//                                        DomainClassesMetadataC16CF0A4C1450DD639D8585C47D62814.io_alw_css_cashflowconsumer_domain_mapper_rule_RevisionTypeResolverContext_Metadata_INSTANCE.getPropertyIndex("tradeEventAction"),
//                                        io.alw.css.cashflowconsumer.domain.mapper.rule.P99.LambdaExtractor996B964FEE005ABD60C242A8734D168E.INSTANCE,
//                                        "FoTradeEventAction.ADD")).expr("GENERATED_1EBF666B4CEE8F4E63214C6AF9AF8A28",
//                                io.alw.css.cashflowconsumer.domain.mapper.rule.P91.LambdaPredicate91ED6D2C1B8A3C265FD669C379E4871C.INSTANCE,
//                                D.alphaIndexedBy(io.alw.css.domain.cashflow.FoTradeEventType.class,
//                                        org.drools.model.Index.ConstraintType.EQUAL,
//                                        DomainClassesMetadataC16CF0A4C1450DD639D8585C47D62814.io_alw_css_cashflowconsumer_domain_mapper_rule_RevisionTypeResolverContext_Metadata_INSTANCE.getPropertyIndex("tradeEventType"),
//                                        io.alw.css.cashflowconsumer.domain.mapper.rule.P2E.LambdaExtractor2ECAA204DFA39888AC434DAAEB518C39.INSTANCE,
//                                        "FoTradeEventType.REBOOK")),
//                        D.on(var_$c).execute(io.alw.css.cashflowconsumer.domain.mapper.rule.P3E.LambdaConsequence3EFAD8A70ECFE2CCC497AABB609B0897.INSTANCE));
//        return rule;
//    }
//}
