package io.alw.css.cashflowconsumer.processor.rule;

public sealed interface RuleDefinition permits CommonRules, OptionRules, MmRules, RepoRules, NdfRules, BondRules {
}
