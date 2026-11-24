package tools.vitruv.neojoin.common;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcoreFactory;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ClassifierNamesTest {

    private static final EcoreFactory FACTORY = EcoreFactory.eINSTANCE;

    private static EPackage createPackage(String name, String nsUri) {
        var pack = FACTORY.createEPackage();
        pack.setName(name);
        pack.setNsPrefix(name);
        pack.setNsURI(nsUri);
        return pack;
    }

    private static EClass addClass(EPackage pack, String name) {
        var clazz = FACTORY.createEClass();
        clazz.setName(name);
        pack.getEClassifiers().add(clazz);
        return clazz;
    }

    private static EDataType addDataType(EPackage pack, String name) {
        var dataType = FACTORY.createEDataType();
        dataType.setName(name);
        pack.getEClassifiers().add(dataType);
        return dataType;
    }

    /**
     * Adds a reference from a new class of the first package to the given class, so that the package of the target
     * class is referenced by the first package.
     */
    private static void addReferenceTo(EPackage pack, EClass target) {
        var reference = FACTORY.createEReference();
        reference.setName("target");
        reference.setEType(target);
        addClass(pack, "Holder").getEStructuralFeatures().add(reference);
    }

    @Test
    void classifierWithUniqueNameIsWrittenWithSimpleName() {
        var pack = createPackage("main", "http://example.org/main");
        var dog = addClass(pack, "Dog");
        addClass(pack, "Cat");

        var names = new ClassifierNames(Map.of(pack, "m"), List.of(pack));

        assertThat(names.write(dog)).isEqualTo(new ClassifierNames.WrittenName("Dog", null));
    }

    @Test
    void classifierWithAmbiguousSimpleNameIsWrittenWithImportAlias() {
        var packA = createPackage("supplier", "http://example.org/supplier-a");
        var packB = createPackage("supplier", "http://example.org/supplier-b");
        var foodA = addClass(packA, "Food");
        var foodB = addClass(packB, "Food");

        var names = new ClassifierNames(Map.of(packA, "a", packB, "b"), List.of(packA, packB));

        assertThat(names.write(foodA)).isEqualTo(new ClassifierNames.WrittenName("a.Food", null));
        assertThat(names.write(foodB)).isEqualTo(new ClassifierNames.WrittenName("b.Food", null));
    }

    @Test
    void classifierInPackageWithoutImportIsWrittenWithPackageName() {
        var main = createPackage("main", "http://example.org/main");
        var other = createPackage("other", "http://example.org/other");
        addClass(main, "Food");
        var otherFood = addClass(other, "Food");
        addReferenceTo(main, otherFood);

        var names = new ClassifierNames(Map.of(main, "m"), List.of(main));

        assertThat(names.write(otherFood)).isEqualTo(new ClassifierNames.WrittenName("other.Food", null));
    }

    @Test
    void classifierIsAmbiguousThroughReferencedPackage() {
        var main = createPackage("main", "http://example.org/main");
        var other = createPackage("other", "http://example.org/other");
        var mainFood = addClass(main, "Food");
        addReferenceTo(main, addClass(other, "Food"));

        var names = new ClassifierNames(Map.of(main, "m"), List.of(main));

        assertThat(names.write(mainFood)).isEqualTo(new ClassifierNames.WrittenName("m.Food", null));
    }

    @Test
    void namespaceUriIsAddedIfQualifiedNameIsAmbiguousToo() {
        var packA = createPackage("supplier", "http://example.org/supplier-a");
        var packB = createPackage("supplier", "http://example.org/supplier-b");
        var foodA = addClass(packA, "Food");
        addClass(packB, "Food");

        var names = new ClassifierNames(Map.of(), List.of(packA, packB));

        var written = names.write(foodA);
        assertThat(written)
            .isEqualTo(new ClassifierNames.WrittenName("supplier.Food", "http://example.org/supplier-a"));
        assertThat(written.plain()).isEqualTo("supplier.Food (http://example.org/supplier-a)");
        assertThat(written.quoted()).isEqualTo("'supplier.Food' (http://example.org/supplier-a)");
        assertThat(written.quotedWithMember("name")).isEqualTo("'supplier.Food::name' (http://example.org/supplier-a)");
    }

    @Test
    void classAndDataTypeWithSameNameAreNotAmbiguous() {
        var pack = createPackage("main", "http://example.org/main");
        var clazz = addClass(pack, "Text");
        var dataType = addDataType(pack, "Text");

        var names = new ClassifierNames(Map.of(pack, "m"), List.of(pack));

        assertThat(names.write(clazz).name()).isEqualTo("Text");
        assertThat(names.write(dataType).name()).isEqualTo("Text");
    }

    @Test
    void classifierInSubPackageIsWrittenWithSubPackageNames() {
        var root = createPackage("root", "http://example.org/root");
        var sub = createPackage("sub", "http://example.org/sub");
        root.getESubpackages().add(sub);
        addClass(root, "Food");
        var subFood = addClass(sub, "Food");

        var names = new ClassifierNames(Map.of(root, "r"), List.of(root));

        assertThat(names.write(subFood)).isEqualTo(new ClassifierNames.WrittenName("r.sub.Food", null));
    }

}
