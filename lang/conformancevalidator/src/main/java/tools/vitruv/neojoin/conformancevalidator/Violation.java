package tools.vitruv.neojoin.conformancevalidator;

import org.eclipse.emf.compare.Diff;

/**
 * Represents a single conformance rule violation from in Phase II.
 *
 * @param ruleId   the identifier of the violated rule
 * @param message  human-readable description of the violation
 * @param category whether this violation can be resolved by adjusting the NeoJoin query,
 *                 or requires manual intervention
 * @param diff     the EMF Compare diff that triggered this violation
 */
public record Violation(String ruleId, String message, Category category, Diff diff) {

    public enum Category {
        BREAKING_RESOLVABLE,
        BREAKING_NOT_RESOLVABLE
    }

    public Category getCategory() {
        return category;
    }

    @Override
    public String toString() {
        return "[%s] %s".formatted(ruleId, message);
    }
}
