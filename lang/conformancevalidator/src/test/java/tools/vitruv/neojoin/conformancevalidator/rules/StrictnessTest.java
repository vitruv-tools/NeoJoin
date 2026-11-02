package tools.vitruv.neojoin.conformancevalidator.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

import tools.vitruv.neojoin.conformancevalidator.Category;

/**
 * Unit tests for the categories each strictness allows. The selection decides which rules
 * Phase II evaluates at all, so a wrong set here would silently change every verdict.
 */
class StrictnessTest {

    @Test
    void subtypeEvaluatesOnlyTheBreakingCategories() {
        Set<Category> allowed = Strictness.SUBTYPE.getAllowedCategories();

        assertTrue(allowed.contains(Category.BREAKING_RESOLVABLE));
        assertTrue(allowed.contains(Category.BREAKING_NOT_RESOLVABLE));
        assertFalse(allowed.contains(Category.NON_BREAKING));
        assertEquals(2, allowed.size());
    }

    @Test
    void exactMatchEvaluatesEveryCategory() {
        Set<Category> allowed = Strictness.EXACT_MATCH.getAllowedCategories();

        assertTrue(allowed.contains(Category.BREAKING_RESOLVABLE));
        assertTrue(allowed.contains(Category.BREAKING_NOT_RESOLVABLE));
        assertTrue(allowed.contains(Category.NON_BREAKING));
        assertEquals(Category.values().length, allowed.size());
    }

    @Test
    void everythingSubtypeEvaluatesIsAlsoEvaluatedByExactMatch() {
        assertTrue(Strictness.EXACT_MATCH.getAllowedCategories()
            .containsAll(Strictness.SUBTYPE.getAllowedCategories()));
    }
}
