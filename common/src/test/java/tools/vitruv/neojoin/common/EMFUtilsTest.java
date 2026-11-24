package tools.vitruv.neojoin.common;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.EPackageRegistryImpl;
import org.eclipse.emf.ecore.resource.impl.ResourceImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xml.type.XMLTypePackage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.NoSuchElementException;

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
        assertThat(EMFUtils.getAllEPackages(root).toList()).containsExactly(root, sub);
        assertThat(EMFUtils.getAllEClassifiers(root).toList())
            .containsExactly(animal, dataType, dog, puppy);
        assertThat(EMFUtils.getAllEDataTypes(root).toList()).containsExactly(dataType);
    }

    @Test
    void referencedRootPackagesFollowSuperTypesAndFeatureTypesTransitively() {
        var pet = createClass("Pet");
        var friend = FACTORY.createEReference();
        friend.setName("friend");
        friend.setEType(puppy); // a class in a sub package: its root package is referenced
        pet.getEStructuralFeatures().add(friend);
        var other = createPackage("other", "http://example.org/other");
        other.getEClassifiers().add(pet);

        var cat = createClass("Cat");
        cat.getESuperTypes().add(pet);
        var third = createPackage("third", "http://example.org/third");
        third.getEClassifiers().add(cat);

        createPackage("unrelated", "http://example.org/unrelated").getEClassifiers().add(createClass("Stone"));

        assertThat(EMFUtils.getReferencedRootPackages(List.of(third))).containsExactly(third, other, root);
        assertThat(EMFUtils.getReferencedRootPackages(List.of(sub))).containsExactly(root);
    }

    @Test
    void referencedRootPackagesIgnoreUnresolvedProxies() {
        var missing = createClass("Missing");
        ((InternalEObject) missing).eSetProxyURI(URI.createURI("http://example.org/missing#//Missing"));
        var reference = FACTORY.createEReference();
        reference.setName("missing");
        reference.setEType(missing);
        var holder = createClass("Holder");
        holder.getEStructuralFeatures().add(reference);
        var pack = createPackage("holder", "http://example.org/holder");
        pack.getEClassifiers().add(holder);

        assertThat(EMFUtils.getReferencedRootPackages(List.of(pack))).containsExactly(pack);
    }

    @Test
    void packagesOfAResourceAreFoundUnlessTheResourceIsEmpty() {
        var resource = new ResourceImpl();
        assertThatThrownBy(() -> EMFUtils.getAllEPackages(resource)).isInstanceOf(NoSuchElementException.class);

        resource.getContents().add(root);
        assertThat(EMFUtils.getAllEPackages(resource).toList()).containsExactly(root, sub);
    }

    @Test
    void rootPackageAndFullyQualifiedNameFollowTheSuperPackages() {
        assertThat(EMFUtils.getRootPackage(sub)).isSameAs(root);
        assertThat(EMFUtils.getRootPackage(root)).isSameAs(root);
        assertThat(EMFUtils.getFullyQualifiedNameAsStream(sub).toList())
            .containsExactly("root", "sub");
    }

    @Test
    void instancesOfAClassIncludeOrExcludeSubClasses() {
        var resource = new ResourceImpl();
        var dogInstance = EcoreUtil.create(dog);
        var puppyInstance = EcoreUtil.create(puppy);
        resource.getContents().add(dogInstance);
        resource.getContents().add(puppyInstance);

        assertThat(EMFUtils.getAllInstances(resource, dog).toList())
            .containsExactly(dogInstance, puppyInstance);
        assertThat(EMFUtils.getAllDirectInstances(resource, dog).toList())
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

    /**
     * A given package is used even if a global package has its namespace URI: the models must be loaded with the
     * package that the query uses, also if the global package registered itself only after the meta-models were
     * collected (for example a copy of Xtext's {@code JavaVMTypes} on the meta-model path).
     */
    @Test
    void createdResourceSetPrefersGivenPackagesToGlobalOnes() {
        var copy = createPackage("ecore", EcorePackage.eNS_URI);
        var registry = new EPackageRegistryImpl();
        registry.put(root.getNsURI(), root);
        registry.put(EcorePackage.eNS_URI, copy);

        var packages = EMFUtils.createResourceSet(registry).getPackageRegistry();

        assertThat(packages.getEPackage(root.getNsURI())).isSameAs(root);
        assertThat(packages.getEPackage(EcorePackage.eNS_URI)).isSameAs(copy);
        assertThat(packages.getEPackage(XMLTypePackage.eNS_URI)).isSameAs(XMLTypePackage.eINSTANCE);
    }

    /**
     * EMF opens an unknown namespace URI ({@code http://example.org/unknown}) as the location of its meta-model.
     */
    @Test
    void createdResourceSetDoesNotAccessTheNetwork() {
        var converter = EMFUtils.createResourceSet().getURIConverter();
        var uri = URI.createURI("http://example.org/unknown");

        assertThatThrownBy(() -> converter.createInputStream(uri))
            .isInstanceOf(IOException.class)
            .hasMessage("Models are not loaded from the network: http://example.org/unknown");
        assertThatThrownBy(() -> converter.createOutputStream(uri))
            .isInstanceOf(IOException.class)
            .hasMessage("Models are not saved to the network: http://example.org/unknown");
        assertThat(converter.exists(uri, null)).isFalse();
    }

    @Test
    void savedModelsReferenceLocalElementsByFragment(@TempDir Path directory) throws IOException {
        var reference = FACTORY.createEReference();
        reference.setName("friend");
        reference.setEType(animal);
        dog.getEStructuralFeatures().add(reference);
        var file = directory.resolve("root.ecore");

        EMFUtils.save(URI.createFileURI(file.toString()), root);

        // loading shows that the file is a valid model; the relative reference does not contain the file name
        var loaded = EMFUtils.createResourceSet().getResource(URI.createFileURI(file.toString()), true);
        assertThat(loaded.getContents()).hasSize(1);
        assertThat(Files.readString(file)).doesNotContain("root.ecore");
    }

}
