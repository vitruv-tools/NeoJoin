package tools.vitruv.neojoin.conformancevalidator;

import org.eclipse.emf.compare.Diff;

/**
 * This Violation record represents a single conformance rule violation from in Phase II.
 *
 * @param ruleId   the identifier of the violated rule
 * @param message  description of the violation
 * @param category whether this violation can be resolved by adjusting the NeoJoin query,
 *                 or requires manual intervention
 * @param diff     the EMF Compare diff that triggered this violation
 */
public record Violation(String ruleId, String message, Category category, Diff diff) {

    public enum Category {
        BREAKING_RESOLVABLE,
        BREAKING_NOT_RESOLVABLE,
        NON_BREAKING
    }

    public Category getCategory() {
        return category;
    }

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
