package tools.vitruv.neojoin.generation;

import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EPackage;
import org.junit.jupiter.api.Test;
import tools.vitruv.neojoin.AbstractKennelTest;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Only checks the mapping of inheritance to the generated view type. The structure of the AQR is covered by the AQR
 * tests.
 */
class InheritanceGenerationTest extends AbstractKennelTest {

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
     * Abstract flag, super types, and overriding features are inherited and not duplicated.
     */
    @Test
    void abstractFlagAndSuperTypesAreGeneratedAndOverridesAreNotDuplicated() {
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
    }

    /**
     * A feature that is inherited via several super classes from a common ancestor exists once in the generated class,
     * which Ecore requires.
     */
    @Test
    void diamondInheritanceGeneratesInheritedFeatureOnce() {
        var pack = generate("""
            abstract A {
                name: EString
            }

            abstract B extends A {
                name: EString
            }

            abstract C extends A {
                name: EString
            }

            from Dog d create D extends B, C {
                name = d.name
            }
            """);

        assertThat(names(eClass(pack, "D").getEAllStructuralFeatures())).containsExactly("name");
        assertThat(eClass(pack, "D").getESuperTypes()).containsExactly(eClass(pack, "B"), eClass(pack, "C"));
    }

    /**
     * The enum type of a feature that is declared in an abstract class is copied into the view type and is the type of
     * the feature.
     */
    @Test
    void enumTypeOfAbstractFeatureIsCopiedIntoViewType() {
        var pack = generate("""
            abstract Specialized {
                specialty: Specialty
            }

            from Vet v create Doctor extends Specialized {
                specialty = v.specialty
            }
            """);

        var specialty = pack.getEClassifier("Specialty");
        assertThat(specialty).isInstanceOf(EEnum.class);
        assertThat(eClass(pack, "Specialized").getEStructuralFeature("specialty").getEType()).isSameAs(specialty);
    }

}
