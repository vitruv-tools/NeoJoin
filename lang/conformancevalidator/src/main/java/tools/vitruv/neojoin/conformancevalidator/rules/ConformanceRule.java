package tools.vitruv.neojoin.conformancevalidator.rules;

import java.util.Optional;

import org.eclipse.emf.compare.Diff;

import tools.vitruv.neojoin.conformancevalidator.Violation;

public abstract class ConformanceRule {
    private final String ruleId;
    private final Violation.Category category;

    protected ConformanceRule(String ruleId, Violation.Category category) {
        this.ruleId = ruleId;
        this.category = category;
    }

    public String getRuleId() { return ruleId; }
    public Violation.Category getCategory() { return category; }

    public abstract Optional<Violation> check(Diff diff);

    protected Violation violation(String message, Diff diff) {
        return new Violation(ruleId, message, category, diff);
    }
}