package tools.vitruv.neojoin.common;

import org.eclipse.emf.common.util.BasicDiagnostic;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.impl.EPackageRegistryImpl;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceImpl;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EMFUtilsTest {

    private static final EcoreFactory FACTORY = EcoreFactory.eINSTANCE;

    private EPackage root;
    private EPackage sub;
    private EClass animal;
    private EClass dog;
    private EClass puppy;
    private EDataType dataType;

    @BeforeEach
    void createPackages() {
        root = createPackage("root", "http://example.org/root");
        sub = createPackage("sub", "http://example.org/sub");
        root.getESubpackages().add(sub);

        animal = createClass("Animal");
        dog = createClass("Dog");
        puppy = createClass("Puppy");
        dog.getESuperTypes().add(animal);
        puppy.getESuperTypes().add(dog);
        root.getEClassifiers().add(animal);
        sub.getEClassifiers().add(dog);
        sub.getEClassifiers().add(puppy);

        dataType = FACTORY.createEDataType();
        dataType.setName("Text");
        dataType.setInstanceClassName("java.lang.String");
        root.getEClassifiers().add(dataType);
    }

    private static EPackage createPackage(String name, String nsUri) {
        var pack = FACTORY.createEPackage();
        pack.setName(name);
        pack.setNsPrefix(name);
        pack.setNsURI(nsUri);
        return pack;
    }

    private static EClass createClass(String name) {
        var clazz = FACTORY.createEClass();
        clazz.setName(name);
        return clazz;
    }

    @Test
    void packagesAndClassifiersIncludeSubpackages() {
        assertThat(EMFUtils.getAllEPackages(root).collect(Collectors.toList())).containsExactly(root, sub);
        assertThat(EMFUtils.getAllEClassifiers(root).collect(Collectors.toList()))
            .containsExactly(animal, dataType, dog, puppy);
        assertThat(EMFUtils.getAllEDataTypes(root).collect(Collectors.toList())).containsExactly(dataType);
    }

    @Test
    void packagesOfAResourceAreFoundUnlessTheResourceIsEmpty() {
        var resource = new ResourceImpl();
        assertThatThrownBy(() -> EMFUtils.getAllEPackages(resource)).isInstanceOf(NoSuchElementException.class);

        resource.getContents().add(root);
        assertThat(EMFUtils.getAllEPackages(resource).collect(Collectors.toList())).containsExactly(root, sub);
    }

    @Test
    void rootPackageAndFullyQualifiedNameFollowTheSuperPackages() {
        assertThat(EMFUtils.getRootPackage(sub)).isSameAs(root);
        assertThat(EMFUtils.getRootPackage(root)).isSameAs(root);
        assertThat(EMFUtils.getFullyQualifiedNameAsStream(sub).collect(Collectors.toList()))
            .containsExactly("root", "sub");
    }

    @Test
    void instancesOfAClassIncludeOrExcludeSubClasses() {
        var resource = new ResourceImpl();
        var dogInstance = EcoreUtil.create(dog);
        var puppyInstance = EcoreUtil.create(puppy);
        resource.getContents().add(dogInstance);
        resource.getContents().add(puppyInstance);

        assertThat(EMFUtils.getAllInstances(resource, dog).collect(Collectors.toList()))
            .containsExactly(dogInstance, puppyInstance);
        assertThat(EMFUtils.getAllDirectInstances(resource, dog).collect(Collectors.toList()))
            .containsExactly(dogInstance);
    }

    @Test
    void nearestSuperClassesAreSearchedLevelByLevel() {
        // only Animal (two levels above Puppy) has a result, Dog (the nearest level) has none
        var result = EMFUtils.findInNearestSuperClasses(
            puppy,
            clazz -> clazz == animal ? List.of("animal") : List.<String>of()
        );
        assertThat(result).containsExactly("animal");

        // the nearest level with a result wins
        var nearest = EMFUtils.findInNearestSuperClasses(puppy, clazz -> List.of(clazz.getName()));
        assertThat(nearest).containsExactly("Dog");

        assertThat(EMFUtils.findInNearestSuperClasses(animal, clazz -> List.of(clazz.getName()))).isEmpty();
    }

    @Test
    void availablePackagesIncludeTheDelegateRegistry() {
        var delegate = new EPackageRegistryImpl();
        delegate.put(root.getNsURI(), root);
        var registry = new EPackageRegistryImpl(delegate);
        registry.put(sub.getNsURI(), sub);

        assertThat(EMFUtils.collectAvailablePackages(registry)).containsExactlyInAnyOrder(root, sub);
    }

    @Test
    void diagnosticSeverityTextNamesTheHighestSeverity() {
        assertThat(EMFUtils.diagnosticSeverityText(new BasicDiagnostic(Diagnostic.ERROR, "s", 0, "m", null)))
            .isEqualTo("ERROR");
        assertThat(EMFUtils.diagnosticSeverityText(new BasicDiagnostic(Diagnostic.WARNING, "s", 0, "m", null)))
            .isEqualTo("WARNING");
        assertThat(EMFUtils.diagnosticSeverityText(new BasicDiagnostic(Diagnostic.INFO, "s", 0, "m", null)))
            .isEqualTo("INFO");
        assertThat(EMFUtils.diagnosticSeverityText(Diagnostic.OK_INSTANCE)).isEqualTo("OK");
    }

    @Test
    void savedModelsReferenceLocalElementsByFragment(@TempDir Path directory) throws IOException {
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("ecore", new EcoreResourceFactoryImpl());
        var reference = FACTORY.createEReference();
        reference.setName("friend");
        reference.setEType(animal);
        dog.getEStructuralFeatures().add(reference);
        var file = directory.resolve("root.ecore");

        EMFUtils.save(URI.createFileURI(file.toString()), root);

        // loading shows that the file is a valid model; the relative reference does not contain the file name
        var loaded = new ResourceSetImpl().getResource(URI.createFileURI(file.toString()), true);
        assertThat(loaded.getContents()).hasSize(1);
        assertThat(Files.readString(file)).doesNotContain("root.ecore");
    }

}
