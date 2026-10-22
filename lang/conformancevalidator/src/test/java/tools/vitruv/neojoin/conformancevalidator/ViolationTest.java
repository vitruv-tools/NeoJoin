package tools.vitruv.neojoin.conformancevalidator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for the report a violation renders. The note about a difference no query can
 * repair is the only part of a message that the record adds by itself, so it is the only part
 * a rule cannot get wrong on its own.
 */
class ViolationTest {

    private static Violation violation(Category category) {
        return new Violation("Rule 9", "the types differ", category, null);
    }

    @Test
    void aBreakingNotResolvableViolationCarriesTheNote() {
        String report = violation(Category.BREAKING_NOT_RESOLVABLE).toString();

        assertEquals("[Rule 9] the types differ (cannot be resolved by adjusting the query)", report);
    }

    @Test
    void aBreakingResolvableViolationCarriesNoNote() {
        String report = violation(Category.BREAKING_RESOLVABLE).toString();

        assertEquals("[Rule 9] the types differ", report);
        assertFalse(report.contains("cannot be resolved"));
    }

    @Test
    void aNonBreakingViolationCarriesNoNote() {
        String report = violation(Category.NON_BREAKING).toString();

        assertEquals("[Rule 9] the types differ", report);
    }

    @Test
    void theReportNamesTheRuleThatProducedIt() {
        assertTrue(violation(Category.NON_BREAKING).toString().startsWith("[Rule 9]"));
    }

    @Test
    void theCategoryIsTheOneTheViolationWasCreatedWith() {
        assertEquals(Category.BREAKING_RESOLVABLE,
            violation(Category.BREAKING_RESOLVABLE).category());
    }
}
