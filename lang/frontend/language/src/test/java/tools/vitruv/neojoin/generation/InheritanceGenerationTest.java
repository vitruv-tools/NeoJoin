package tools.vitruv.neojoin.generation;

import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EPackage;
import org.junit.jupiter.api.Test;
import tools.vitruv.neojoin.AbstractKennelTest;
import tools.vitruv.neojoin.Constants;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Only checks the mapping of inheritance to the generated view type. The structure of the AQR is covered by the AQR
 * tests.
 */
public class InheritanceGenerationTest extends AbstractKennelTest {

    private EPackage generate(String query) {
        var info = new MetaModelGenerator(parse(query)).generate();
        if (info.diagnostic().getSeverity() >= Diagnostic.ERROR) {
            fail("generated package must pass EMF validation: %s".formatted(info.diagnostic()));
        }
        return info.pack();
    }

    private static EClass eClass(EPackage pack, String name) {
        var classifier = pack.getEClassifier(name);
        assertThat(classifier).as("classifier '%s'", name).isInstanceOf(EClass.class);
        return (EClass) classifier;
    }

    private static List<String> names(Collection<? extends ENamedElement> elements) {
        return elements.stream().map(ENamedElement::getName).toList();
    }

    /**
     * Abstract flag, super types, overriding features are inherited and not duplicated, abstract classes are not
     * referenced from the root.
     */
    @Test
    void generatesAbstractClassesSuperTypesAndNoDuplicatedOverrides() {
        var pack = generate("""
            abstract Named {
                name: EString [1]
            }

            abstract Aged extends Named {
                name: EString
                age: EInt
            }

            abstract Tagged {
                tag: EString
            }

            from Puppy p create Pup extends Aged, Tagged {
                name = p.name
                age = p.age
                tag := "x"
                breed = p.breed
            }
            """);

        var named = eClass(pack, "Named");
        var aged = eClass(pack, "Aged");
        var pup = eClass(pack, "Pup");

        assertThat(named.isAbstract()).isTrue();
        assertThat(aged.isAbstract()).isTrue();
        assertThat(pup.isAbstract()).isFalse();
        assertThat(aged.getESuperTypes()).containsExactly(named);
        var tagged = eClass(pack, "Tagged");
        assertThat(tagged.isAbstract()).isTrue();
        assertThat(pup.getESuperTypes()).containsExactly(aged, tagged); // in the order of the query

        assertThat(names(named.getEStructuralFeatures())).containsExactly("name");
        assertThat(names(aged.getEStructuralFeatures())).containsExactly("age");
        assertThat(names(pup.getEStructuralFeatures())).containsExactly("breed");
        assertThat(names(pup.getEAllStructuralFeatures())).containsExactlyInAnyOrder("name", "age", "tag", "breed");
        assertThat(pup.getEStructuralFeature("name")).isSameAs(named.getEStructuralFeature("name"));
        assertThat(pup.getEStructuralFeature("name").getLowerBound()).isEqualTo(1); // modifiers of the overridden feature

        assertThat(names(eClass(pack, Constants.DefaultRootClassName).getEStructuralFeatures()))
            .containsExactly("pups");
    }

    /**
     * Implicit classes keep the super types of their source classes, the inherited features are only declared in the
     * super class.
     */
    @Test
    void implicitSubClassHasSuperTypeAndInheritedFeaturesOnlyOnSuperClass() {
        var pack = generate("""
            from Kennel create
            """);

        var animal = eClass(pack, "Animal");
        var dog = eClass(pack, "Dog");
        var puppy = eClass(pack, "Puppy");

        assertThat(animal.isAbstract()).isTrue();
        assertThat(animal.getESuperTypes()).isEmpty();
        assertThat(names(animal.getEStructuralFeatures())).containsExactly("name");

        assertThat(dog.getESuperTypes()).containsExactly(animal);
        assertThat(names(dog.getEStructuralFeatures())).containsExactly("breed");
        assertThat(names(dog.getEAllStructuralFeatures())).containsExactlyInAnyOrder("name", "breed");

        assertThat(puppy.getESuperTypes()).containsExactly(dog);
        assertThat(names(puppy.getEStructuralFeatures())).containsExactly("age");

        assertThat(names(eClass(pack, Constants.DefaultRootClassName).getEStructuralFeatures()))
            .doesNotContain("animals");
    }

    /**
     * The root can have a super type; enum types of inherited features are mapped.
     */
    @Test
    void rootClassExtendsAbstractClassAndInheritedEnumFeatureIsMapped() {
        var pack = generate("""
            abstract Named {
                name: EString
            }

            abstract Specialized {
                specialty: Specialty
            }

            create root R extends Named {
                name := "r"
            }

            from Vet v create Doctor extends Specialized {
                specialty = v.specialty
            }
            """);

        var named = eClass(pack, "Named");
        var root = eClass(pack, "R");
        assertThat(root.getESuperTypes()).containsExactly(named);
        assertThat(names(root.getEStructuralFeatures())).containsExactly("doctors");
        assertThat(names(root.getEAllStructuralFeatures())).containsExactlyInAnyOrder("name", "doctors");

        var specialty = pack.getEClassifier("Specialty");
        assertThat(specialty).isInstanceOf(EEnum.class);
        assertThat(eClass(pack, "Specialized").getEStructuralFeature("specialty").getEType()).isSameAs(specialty);
        assertThat(eClass(pack, "Doctor").getEStructuralFeature("specialty").getEType()).isSameAs(specialty);
    }

}
