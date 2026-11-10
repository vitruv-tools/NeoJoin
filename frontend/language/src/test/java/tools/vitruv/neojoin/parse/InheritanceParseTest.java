package tools.vitruv.neojoin.parse;

import org.eclipse.xtext.diagnostics.Severity;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static tools.vitruv.neojoin.parse.ParseAssertions.assertThat;

class InheritanceParseTest extends AbstractInheritanceParseTest {

    // ---- grammar and scoping

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
    void classCannotExtendItself() {
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

        assertThat(result).hasExactlyIssues(
            "Cyclic inheritance: 'B' is a sub class of 'A'",
            "Cyclic inheritance: 'A' is a sub class of 'B'"
        );
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

        assertThat(result).hasExactlyIssues(
            "Class extends the root class and inherits all root containment references"
        );
        assertThat(result).hasIssueWithSeverity(
            Severity.WARNING,
            "Class extends the root class and inherits all root containment references"
        );
    }

    // ---- overriding of inherited features

    @Test
    void featureOfSuperClassMustBeOverridden() {
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
    void queryWithoutBodyMustOverrideFeaturesOfSuperClass() {
        var result = parse("""
            abstract Named {
                label: EString
            }

            from Restaurant create Rest extends Named
            """);

        assertThat(result).hasExactlyIssues("Missing override of inherited feature 'label' from 'Named'");
    }

    @Test
    void featureOfSuperClassOfSuperClassMustBeOverridden() {
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
            from extends Animal a create A1 {
                a.name
            }

            from extends Animal a create A2 {
                a.name
            }

            from Kennel k create K {
                k.name
                k.star
            }
            """);

        assertThat(result).hasExactlyIssues(
            "Ambiguous super class for implicit target class of source class 'Dog'. Possible candidates: A1, A2"
        );
    }

    @Test
    void implicitClassWithSuperClassCandidateWithMissingFeatureWarns() {
        var result = parseWithKennel("""
            from Kennel k create KennelT {
                k.name
                k.animals create Animal {
                    name = it.name
                    tag = it.name
                }
                k.dogs
            }
            """);

        var message = "Implicit target class of source class 'Dog' is not linked to super class 'Animal', "
            + "because it has no matching feature (same name, type and modifiers) for: tag";
        assertThat(result).hasExactlyIssues(message);
        assertThat(result).hasIssueWithSeverity(Severity.WARNING, message);
    }

    @Test
    void implicitClassWithSuperClassCandidateWithDifferentFeatureTypeWarns() {
        var result = parseWithKennel("""
            from Dog d create Dog {
                name := d.name.length
            }

            from Kennel k create K {
                k.puppy
            }
            """);

        assertThat(result).hasIssueWithSeverity(
            Severity.WARNING,
            "Implicit target class of source class 'Puppy' is not linked to super class 'Dog', "
                + "because it has no matching feature (same name, type and modifiers) for: name"
        );
    }

    @Test
    void implicitClassWithSuperClassCandidateWithDifferentFeatureModifiersWarns() {
        var result = parseWithKennel("""
            from Dog d create Dog {
                name = d.name
                breed: EString [1] := d.breed
            }

            from Kennel k create K {
                k.puppy
            }
            """);

        assertThat(result).hasIssueWithSeverity(
            Severity.WARNING,
            "Implicit target class of source class 'Puppy' is not linked to super class 'Dog', "
                + "because it has no matching feature (same name, type and modifiers) for: breed"
        );
    }

    @Test
    void implicitClassIsComparedWithExplicitAbstractClassOfSameName() {
        var result = parseWithKennel("""
            abstract Animal {
                name: EString
            }

            from Cat c create C extends Animal {
                name = c.name
            }

            from Kennel k create K {
                k.animals
                k.dogs
            }
            """);

        assertThat(result).hasIssueWithSeverity(
            Severity.WARNING,
            "Implicit target class of source class 'Dog' is not linked to super class 'Animal', "
                + "because it has no matching feature (same name, type and modifiers) for: name"
        );
    }

    @Test
    void fromAbstractClassWithoutExtendsWarns() {
        var result = parseWithKennel("""
            from Animal create Pet
            """);

        assertThat(result).hasIssueWithSeverity(
            Severity.WARNING,
            "Abstract class 'Animal' has no direct instances, use 'extends' to select the instances of its sub classes"
        );
        assertThat(result).hasExactlyIssues(
            "Abstract class 'Animal' has no direct instances, use 'extends' to select the instances of its sub classes"
        );
    }

    @Test
    void joinAbstractClassWithoutExtendsWarns() {
        var result = parseWithKennel("""
            from Kennel k
            join Animal a on a.name == k.name
            create Mix {
                n = k.name
            }
            """);

        assertThat(result).hasExactlyIssues(
            "Abstract class 'Animal' has no direct instances, use 'extends' to select the instances of its sub classes"
        );
    }

    @Test
    void fromExtendsAbstractClassDoesNotWarn() {
        var result = parseWithKennel("""
            from extends Animal create Pet
            """);

        assertThat(result).hasNoIssues();
    }

    @Test
    void explicitSubQueryTypeAcceptsValueOfSubClass() {
        var result = parseWithKennel("""
            from Kennel k create KennelT {
                k.animals create Pet {
                    it.name
                }
                pal: Pet = k.star
            }
            """);

        assertThat(result).hasNoIssues();
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
            "Cyclic inheritance: 'B' is a sub class of 'A'",
            "Cyclic inheritance: 'A' is a sub class of 'B'",
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

    @Test
    void nullableExpressionForRequiredInheritedFeatureWarns() {
        var result = parse("""
            abstract B {
                n: EString [1]
            }

            from Restaurant r create C extends B {
                n := r.name?.toUpperCase
            }
            """);

        assertThat(result).hasIssueWithSeverity(
            Severity.WARNING, "Nullable expression used to initialize non-nullable feature"
        );
    }

    @Test
    void nullableExpressionForOptionalInheritedFeatureDoesNotWarn() {
        var result = parse("""
            abstract B {
                n: EString [?]
            }

            from Restaurant r create C extends B {
                n := r.name?.toUpperCase
            }
            """);

        assertThat(result).hasNoIssues();
    }

    @Test
    void nullableExpressionForRequiredNewFeatureOfClassWithSuperClassWarns() {
        var result = parse("""
            abstract B {
                n: EString [?]
            }

            from Restaurant r create C extends B {
                n := "c"
                m: EString [1] := r.name?.toUpperCase
            }
            """);

        assertThat(result).hasIssueWithSeverity(
            Severity.WARNING, "Nullable expression used to initialize non-nullable feature"
        );
    }

}
