package tools.vitruv.neojoin.parse;

import org.eclipse.xtext.diagnostics.Severity;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static tools.vitruv.neojoin.parse.ParseAssertions.assertThat;

public class InheritanceParseTest extends AbstractInheritanceParseTest {

    // ---- grammar and scoping

    @Test
    void multipleSuperClasses() {
        var result = parse("""
            abstract A {
                a: EString
            }

            abstract B {
                b: EInt
            }

            create C extends A, B {
                a := "x"
                b := 1
            }
            """);

        assertThat(result).hasNoIssues();
    }

    @Test
    void extendsWithoutNameAndBody() {
        var result = parse("""
            abstract Base {
                name: EString
            }

            from Restaurant create extends Base
            """);

        assertThat(result).hasNoIssues();
    }

    @Test
    void extendsQueryByInferredTargetName() {
        var result = parse("""
            from Food create

            from Food f create Cheap extends Food {
                name = f.name
                price = f.price
                type = f.type
            }
            """);

        assertThat(result).hasNoIssues();
    }

    @Test
    void extendsSubQuery() {
        var result = parse("""
            from Restaurant r create {
                r.sells create FoodItem {
                    it.name
                }
            }

            create Special extends FoodItem {
                name := "x"
            }
            """);

        assertThat(result).hasNoIssues();
    }

    @Test
    void unknownSuperClass() {
        var result = parse("""
            create A extends Unknown {
                x := 1
            }
            """);

        assertThat(result).hasExactlyIssues("Unknown cannot be resolved.");
    }

    @Test
    void sourceClassIsNotASuperClass() {
        var result = parse("""
            create A extends Restaurant {
                x := 1
            }
            """);

        assertThat(result).hasExactlyIssues("Restaurant cannot be resolved.");
    }

    // ---- super class relation

    @Test
    void extendsItself() {
        var result = parse("""
            abstract A extends A {}
            """);

        assertThat(result).hasExactlyIssues("A class cannot extend itself");
    }

    @Test
    void cyclicInheritance() {
        var result = parse("""
            abstract A extends B {}
            abstract B extends A {}
            """);

        assertThat(result).hasExactlyIssues("Cyclic inheritance");
        assertThat(result.right().stream().filter(i -> i.getMessage().equals("Cyclic inheritance"))).hasSize(2);
    }

    @Test
    void duplicatedSuperClass() {
        var result = parse("""
            abstract A {}
            abstract B extends A, A {}
            """);

        assertThat(result).hasExactlyIssues("Duplicated super class: A");
    }

    @Test
    void extendingRootWarns() {
        var result = parse("""
            create root R {
                x := 5
            }

            create S extends R {
                x := 6
            }
            """);

        assertThat(result).hasExactlyIssues("Class extends the root class and inherits all root containment references");
        assertThat(result).hasIssueWithSeverity(
            Severity.WARNING,
            "Class extends the root class and inherits all root containment references"
        );
    }

    // ---- overriding of inherited features

    @Test
    void missingOverride() {
        var result = parse("""
            abstract Named {
                name: EString
            }

            from Restaurant r create Rest extends Named {
                r.address
            }
            """);

        assertThat(result).hasExactlyIssues("Missing override of inherited feature 'name' from 'Named'");
    }

    @Test
    void missingOverrideWithoutBody() {
        var result = parse("""
            abstract Named {
                label: EString
            }

            from Restaurant create Rest extends Named
            """);

        assertThat(result).hasExactlyIssues("Missing override of inherited feature 'label' from 'Named'");
    }

    @Test
    void missingOverrideOfTransitiveFeature() {
        var result = parse("""
            abstract A {
                x: EString
            }

            abstract B extends A {
                x: EString
                y: EInt
            }

            from Restaurant r create C extends B {
                x = r.name
            }
            """);

        assertThat(result).hasExactlyIssues("Missing override of inherited feature 'y' from 'B'");
    }

    @Test
    void overrideReferenceWithSubType() {
        var result = parse("""
            abstract Item {
                name: EString
            }

            from Food f create Dish extends Item {
                f.name
            }

            abstract Shop {
                sells: Item [*]
            }

            from Restaurant r create Rest extends Shop {
                sells = r.sells
            }
            """);

        assertThat(result).hasNoIssues();
    }

    @Test
    void overrideWithDifferentDataType() {
        var result = parse("""
            abstract A {
                name: EInt
            }

            from Restaurant r create B extends A {
                r.name
            }
            """);

        assertThat(result).hasExactlyIssues(
            "Type of overriding feature 'name' must be equal to the type of the overridden feature"
        );
    }

    @Test
    void overrideWithDifferentDataTypeInImplicitCopy() {
        var result = parse("""
            abstract Base {
                name: EInt
            }

            from Restaurant create R extends Base
            """);

        assertThat(result).hasExactlyIssues(
            "Type of overriding feature 'name' must be equal to the type of the overridden feature"
        );
    }

    @Test
    void implicitCopiedReferenceOverridesAttribute() {
        var result = parse("""
            abstract Base {
                sells: EString
            }

            from Restaurant create R extends Base
            """);

        assertThat(result).hasExactlyIssues("Reference 'sells' cannot override an attribute");
    }

    @Test
    void overrideMultiValuedFeatureWithSingleValue() {
        var result = parse("""
            abstract B {
                tags: EString [*]
            }

            from Restaurant r create D extends B {
                tags := r.name
            }
            """);

        assertThat(result).hasExactlyIssues("Cannot assign a single value to a multi-valued feature");
    }

    @Test
    void overrideSingleValuedFeatureWithMultipleValues() {
        var result = parse("""
            abstract B {
                n: EString
            }

            from Restaurant r create D extends B {
                n := #[r.name]
            }
            """);

        assertThat(result).hasExactlyIssues("Cannot assign multiple values to a single-valued feature");
    }

    @Test
    void implicitClassWithSeveralCandidatesForSuperClassIsAmbiguous() {
        var result = parseWithKennel("""
            from Animal a create A1 {
                a.name
            }

            from Animal a create A2 {
                a.name
            }

            from Kennel k create K {
                k.name
                k.star create {
                    name
                    breed
                }
            }
            """);

        assertThat(result).hasExactlyIssues("Ambiguous super class for implicit target class of source class 'Dog'. Possible candidates: A1, A2");
    }

    @Test
    void referenceOverridesAttribute() {
        var result = parse("""
            abstract Base {
                sells: EString
            }

            from Restaurant r create Rest extends Base {
                sells = r.sells
            }
            """);

        assertThat(result).hasExactlyIssues("Reference 'sells' cannot override an attribute");
    }

    @Test
    void attributeOverridesReference() {
        var result = parse("""
            abstract Base {
                name: Dish
            }

            from Food create Dish

            from Restaurant r create Rest extends Base {
                name = r.name
            }
            """);

        assertThat(result).hasExactlyIssues("Attribute 'name' cannot override a reference");
    }

    @Test
    void overrideReferenceWithUnrelatedType() {
        var result = parse("""
            abstract Other {}

            abstract Shop {
                sells: Other [*]
            }

            from Food create Dish

            from Restaurant r create Rest extends Shop {
                sells = r.sells
            }
            """);

        assertThat(result).hasExactlyIssues(
            "Value of overriding feature 'sells' cannot be assigned to the type of the overridden feature"
        );
    }

    @Test
    void overrideReferenceWithExplicitSubType() {
        var result = parse("""
            abstract Item {
                name: EString
            }

            from Food f create Dish extends Item {
                f.name
            }

            abstract Shop {
                sells: Item [*]
            }

            from Restaurant r create Rest extends Shop {
                sells: Dish := r.sells
            }
            """);

        assertThat(result).hasExactlyIssues(
            "Type of overriding feature 'sells' must be equal to the type of the overridden feature"
        );
    }

    @Test
    void modifiersOfOverridingFeatureAreIgnored() {
        var result = parse("""
            abstract A {
                name: EString
            }

            from Restaurant r create B extends A {
                name [?] = r.name
            }
            """);

        assertThat(result).hasExactlyIssues("Modifiers of overriding feature 'name' are ignored");
        assertThat(result).hasIssueWithSeverity(Severity.WARNING, "Modifiers of overriding feature 'name' are ignored");
    }

    // ---- explicitly typed references to classes with sub classes

    @Test
    void ambiguousExplicitSuperClass() {
        var result = parse("""
            from Food create Dish

            from Food create Special extends Dish

            from Restaurant r create {
                sells: Dish := r.sells
            }
            """);

        assertThat(result).hasExactlyIssues(
            "Ambiguous target class for source class 'Food'. Possible candidates: Dish, Special"
        );
    }

    @Test
    void unambiguousExplicitSubClass() {
        var result = parse("""
            from Food create Dish

            from Food create Special extends Dish

            from Restaurant r create {
                sells: Special := r.sells
            }
            """);

        assertThat(result).hasNoIssues();
    }

    /**
     * A reference with an explicit class type is reported as a type mismatch if no query selects the inferred class.
     */
    @Test
    void explicitQueryTypeWithoutQueryForInferredClass() {
        var result = parse("""
            from Review create Rev

            from Restaurant r create {
                sells: Rev := r.sells
            }
            """);

        assertThat(result).hasExactlyIssues("Type mismatch: cannot convert from Food to Rev");
    }

    /**
     * Checks with an explicit class type terminate for cyclic inheritance.
     */
    @Test
    void cyclicInheritanceWithExplicitQueryType() {
        var result = parse("""
            abstract A extends B {}
            abstract B extends A {}

            abstract Other {}

            from Food create Dish extends A

            from Restaurant r create {
                sells: Other := r.sells
            }
            """);

        assertThat(result).hasExactlyIssues(
            "Cyclic inheritance",
            "Type mismatch: cannot convert from Food to Other"
        );
    }

    @Test
    void featuresCannotBeDeterminedForInheritanceChecks() {
        var result = parse("""
            abstract A {
                x: EString
            }

            from Restaurant r create B extends A {
                r.doesNotExist
            }
            """);

        assertThat(result).hasExactlyIssues("The method or field doesNotExist is undefined for the type Restaurant");
    }

}
