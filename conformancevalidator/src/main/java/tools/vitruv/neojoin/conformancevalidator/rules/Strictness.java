package tools.vitruv.neojoin.conformancevalidator.rules;

import java.util.Set;

import tools.vitruv.neojoin.conformancevalidator.Category;

/**
 * The requirement a view type has to satisfy regarding its reference metamodel.
 *
 * The strictness decides which rule categories Phase II evaluates, and therefore which
 * differences count as a violation.
 */
public enum Strictness {

    /**
     * The view type has to reproduce the reference exactly, so every rule counts.
     */
    EXACT_MATCH,

    /**
     * The view type has to be usable wherever the reference is expected, so only the
     * differences that break the conformance relation count.
     */
    SUBTYPE;

    /**
     * Returns the rule categories this strictness evaluates.
     *
     * @return the categories whose rules are evaluated under this strictness
     */
    public Set<Category> getAllowedCategories() {
        return switch (this) {
            case SUBTYPE -> Set.of(
                Category.BREAKING_RESOLVABLE,
                Category.BREAKING_NOT_RESOLVABLE);
            case EXACT_MATCH -> Set.of(
                Category.BREAKING_RESOLVABLE,
                Category.BREAKING_NOT_RESOLVABLE,
                Category.NON_BREAKING);
        };
    }
}
