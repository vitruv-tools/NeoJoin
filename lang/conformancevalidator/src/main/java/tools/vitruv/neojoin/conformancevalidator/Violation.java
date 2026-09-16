package tools.vitruv.neojoin.conformancevalidator;

import org.eclipse.emf.compare.Diff;

/**
 * A single violation of the conformance relation, reported by one rule for one difference.
 *
 * @param ruleId the identifier of the rule that reported the violation
 * @param message the description of the violation
 * @param category the category of the violation
 * @param diff the Diff that triggered the violation
 */
public record Violation(String ruleId, String message, Category category, Diff diff) {

    /**
     * Returns the category of the rule that reported the violation.
     *
     * @return the category of the violation
     */
    public Category getCategory() {
        return category;
    }

    /**
     * Puts together a violation message.
     *
     * @return the rule id and the message
     */
    @Override
    public String toString() {
        return switch (category) {
            case BREAKING_NOT_RESOLVABLE ->
                "[%s] %s (cannot be resolved by adjusting the query)".formatted(ruleId, message);
            default ->
                "[%s] %s".formatted(ruleId, message);
        };
    }
}
