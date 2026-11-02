package tools.vitruv.neojoin.conformancevalidator;

/**
 * The category of a conformance rule, and therefore of every violation it reports.
 * It states how a difference affects the conformance relation.
 * {@link tools.vitruv.neojoin.conformancevalidator.rules.Strictness} selects
 * which of these categories are evaluated.
 */
public enum Category {

    /**
     * The difference does not break the conformance relation.
     */
    NON_BREAKING,

    /**
     * The difference breaks the conformance relation and can be repaired by adjusting the query.
     */
    BREAKING_RESOLVABLE,

    /**
     * The difference breaks the conformance relation and cannot be repaired by adjusting the
     * query.
     */
    BREAKING_NOT_RESOLVABLE
}
