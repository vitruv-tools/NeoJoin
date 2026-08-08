package tools.vitruv.neojoin.conformancevalidator.rules;

import java.util.Set;

import tools.vitruv.neojoin.conformancevalidator.Violation;

/**
 * This Strictness Enum represents the categories of rules the view type has to conform to.
 */
public enum Strictness {

    EXACT_MATCH,
    SUBTYPE;

    public Set<Violation.Category> getAllowedCategories() {
        return switch (this) {
        case SUBTYPE -> Set.of(
            Violation.Category.BREAKING_RESOLVABLE,
            Violation.Category.BREAKING_NOT_RESOLVABLE);
        case EXACT_MATCH -> Set.of(
            Violation.Category.BREAKING_RESOLVABLE,
            Violation.Category.BREAKING_NOT_RESOLVABLE,
            Violation.Category.NON_BREAKING);
        };
    }

}
