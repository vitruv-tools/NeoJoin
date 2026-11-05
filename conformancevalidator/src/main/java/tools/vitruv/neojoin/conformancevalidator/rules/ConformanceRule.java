package tools.vitruv.neojoin.conformancevalidator.rules;

import java.util.Optional;

import org.eclipse.emf.compare.Diff;

import tools.vitruv.neojoin.conformancevalidator.Category;
import tools.vitruv.neojoin.conformancevalidator.Violation;

/**
 * Base class of every conformance rule of Phase II.
 * A rule is written against one specific kind of difference and carries a fixed identifier
 * and a fixed category.
 */
public abstract class ConformanceRule {

    private final String ruleId;
    private final Category category;

    /**
     * Creates a rule with the identifier and category.
     *
     * @param ruleId the identifier under which the rule is reported
     * @param category how a difference this rule matches affects the conformance relation
     */
    protected ConformanceRule(String ruleId, Category category) {
        this.ruleId = ruleId;
        this.category = category;
    }

    /**
     * Getter for the id.
     *
     * @return the identifier of the rule
     */
    public String getRuleId() {
        return ruleId;
    }

    /**
     * Getter of the category
     *
     * @return how a difference this rule matches affects the conformance relation
     */
    public Category getCategory() {
        return category;
    }

    /**
     * Evaluates the given difference against this rule.
     *
     * @param diff the difference reported by Phase I
     * @return the violation if this rule applies to the difference, an empty optional otherwise
     */
    public abstract Optional<Violation> check(Diff diff);

    /**
     * Builds a violation for this rule.
     *
     * @param message the description of what was found
     * @param diff the difference that triggered the violation
     * @return the violation
     */
    protected Violation violation(String message, Diff diff) {
        return new Violation(ruleId, message, category, diff);
    }
}
