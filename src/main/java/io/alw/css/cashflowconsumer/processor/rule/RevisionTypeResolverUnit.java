package io.alw.css.cashflowconsumer.processor.rule;

import org.drools.ruleunits.api.DataSource;
import org.drools.ruleunits.api.DataStore;
import org.drools.ruleunits.api.RuleUnitData;

public class RevisionTypeResolverUnit implements RuleUnitData {
    private final DataStore<RevisionTypeResolverContext> context;

    public RevisionTypeResolverUnit() {
        this(DataSource.createStore());
    }

    public RevisionTypeResolverUnit(DataStore<RevisionTypeResolverContext> context) {
        this.context = context;
    }

    public DataStore<RevisionTypeResolverContext> getContext() {
        return context;
    }

}
