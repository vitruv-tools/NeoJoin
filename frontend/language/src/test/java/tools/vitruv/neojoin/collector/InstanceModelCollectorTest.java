package tools.vitruv.neojoin.collector;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcorePackage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.io.TempDir;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.eclipse.xtext.diagnostics.Severity.ERROR;
import static org.eclipse.xtext.diagnostics.Severity.WARNING;

class InstanceModelCollectorTest {

    private String testName;

    private EPackage.Registry registry;

    @BeforeEach
    void collectMetaModel(TestInfo testInfo) {
        testName = testInfo.getTestMethod().orElseThrow().getName();
        registry = new PackageModelCollector(toFilePath("/models/eobject-ref.ecore")).collect().right();
    }

    /**
     * Source meta-models may reference Ecore classes, so their instances may reference elements of Ecore.
     */
    @Test
    void resolvesReferencesToEcoreElements() {
        var result = new InstanceModelCollector(testFolder().toString(), registry).collect();

        assertThat(result.left()).isEmpty();
        var holder = result.right().get(registry.getEPackage("http://vitruv.tools/eobjectref")).getContents().get(0);
        assertThat(holder.eGet(holder.eClass().getEStructuralFeature("target"))).isSameAs(EcorePackage.Literals.ECLASS);
    }

    /**
     * The meta-model of the restaurants is unknown, the one of the package (Ecore) is only in the global registry:
     * queries cannot import either of them.
     */
    @Test
    void ignoresInstanceModelsOfMetaModelsOutsideModelPath() {
        var restaurants = testFolder().resolve("restaurants.xmi");
        var pack = testFolder().resolve("package.xmi");

        var result = new InstanceModelCollector(testFolder().toString(), registry).collect();

        assertThat(result.left()).extracting(CollectorIssue::severity, Object::toString).containsExactly(
            tuple(WARNING, "Ignoring the instance model '" + pack.toUri() + "': it uses the meta-model "
                + "'http://www.eclipse.org/emf/2002/Ecore', which is not on the meta-model path."),
            tuple(WARNING, "Ignoring the instance model '" + restaurants.toUri() + "': it uses the meta-model "
                + "'http://example.org/restaurant', which is not on the meta-model path.")
        );
        assertThat(result.right()).containsOnlyKeys(registry.getEPackage("http://vitruv.tools/eobjectref"));
    }

    @Test
    void reportsMultipleInstanceModelsOfOneMetaModel() {
        var first = testFolder().resolve("a.xmi");
        var second = testFolder().resolve("b.xmi");

        var result = new InstanceModelCollector(testFolder().toString(), registry).collect();

        assertThat(result.left()).extracting(CollectorIssue::severity, Object::toString).containsExactly(
            tuple(ERROR, "Found several instance models of the meta-model 'http://vitruv.tools/eobjectref': '"
                + first.toUri() + "' and '" + second.toUri() + "'.")
        );
    }

    /**
     * An instance model can contain the roots of several meta-models within {@code xmi:XMI}.
     */
    @Test
    void usesInstanceModelForTheMetaModelsOfAllRoots() {
        var bothRegistry = new PackageModelCollector(
            toFilePath("/models/eobject-ref.ecore") + ";" + toFilePath("/models/restaurant.ecore")
        ).collect().right();

        var result = new InstanceModelCollector(testFolder().toString(), bothRegistry).collect();

        assertThat(result.left()).isEmpty();
        assertThat(result.right()).containsOnlyKeys(
            bothRegistry.getEPackage("http://vitruv.tools/eobjectref"),
            bothRegistry.getEPackage("http://example.org/restaurant")
        );
    }

    /**
     * The transformation looks up the instance model of a source class by the root package of its meta-model.
     */
    @Test
    void assignsInstanceModelOfSubPackageToItsRootPackage() {
        var zooRegistry = collectZooMetaModel();

        var result = new InstanceModelCollector(testFolder().toString(), zooRegistry).collect();

        assertThat(result.left()).isEmpty();
        assertThat(result.right()).containsOnlyKeys(zooRegistry.getEPackage("http://example.org/zoo"));
    }

    @Test
    void namesTheInstanceModelPathInProblemsOfItsEntries(@TempDir Path dir) {
        var missing = dir.resolve("missing");

        var result = new InstanceModelCollector(missing.toString(), registry).collect();

        assertThat(result.left()).extracting(CollectorIssue::severity, Object::toString).containsExactly(
            tuple(WARNING, "Ignoring '" + missing + "' on the instance model path: the file or directory does not "
                + "exist.")
        );
    }

    /**
     * The broken file ends within the start tag of its root element, so its meta-model is unknown.
     */
    @Test
    void ignoresInstanceModelsThatCannotBeLoaded() {
        var broken = testFolder().resolve("b.xmi");
        var empty = testFolder().resolve("c.xmi");

        var result = new InstanceModelCollector(testFolder().toString(), registry).collect();

        assertThat(result.left()).extracting(CollectorIssue::severity).containsOnly(WARNING);
        assertThat(result.left()).map(Object::toString).satisfiesExactly(
            message -> assertThat(message).startsWith("Cannot load '" + broken.toUri() + "': "),
            message -> assertThat(message).isEqualTo(
                "Cannot load '" + empty.toUri() + "': the file contains no model elements."
            )
        );
        assertThat(result.right()).containsOnlyKeys(registry.getEPackage("http://vitruv.tools/eobjectref"));
    }

    /**
     * A file that cannot be loaded may still be meant as the instance model of a meta-model, which its root element
     * (also within {@code xmi:XMI}) shows: the first file has an element of an unknown meta-model inside, the second
     * one is not well-formed.
     */
    @Test
    void reportsMultipleInstanceModelsOfOneMetaModelAlsoIfTheyCannotBeLoaded() {
        var first = testFolder().resolve("a.xmi");
        var second = testFolder().resolve("b.xmi");
        var third = testFolder().resolve("c.xmi");

        var result = new InstanceModelCollector(testFolder().toString(), registry).collect();

        assertThat(result.left()).extracting(CollectorIssue::severity).containsExactly(WARNING, WARNING, ERROR, ERROR);
        assertThat(result.left()).filteredOn(issue -> issue.severity() == ERROR).map(Object::toString).containsExactly(
            "Found several instance models of the meta-model 'http://vitruv.tools/eobjectref': '" + first.toUri()
                + "' and '" + second.toUri() + "'.",
            "Found several instance models of the meta-model 'http://vitruv.tools/eobjectref': '" + first.toUri()
                + "' and '" + third.toUri() + "'."
        );
    }

    /**
     * A file that cannot be loaded counts for the root package of its root element too, like a loaded one.
     */
    @Test
    void reportsInstanceModelOfSubPackageThatCannotBeLoadedAsInstanceModelOfItsRootPackage() {
        var zooRegistry = collectZooMetaModel();
        var zoo = testFolder().resolve("a.xmi");
        var animal = testFolder().resolve("b.xmi");

        var result = new InstanceModelCollector(testFolder().toString(), zooRegistry).collect();

        assertThat(result.left()).filteredOn(issue -> issue.severity() == ERROR).map(Object::toString).containsExactly(
            "Found several instance models of the meta-model 'http://example.org/zoo': '" + zoo.toUri() + "' and '"
                + animal.toUri() + "'."
        );
    }

    /**
     * Collects the meta-model {@code zoo.ecore} in the folder of the test; the instance model collector skips it,
     * because it only loads {@code .xmi} files.
     */
    private EPackage.Registry collectZooMetaModel() {
        return new PackageModelCollector(testFolder().resolve("zoo.ecore").toString()).collect().right();
    }

    /**
     * Returns the resource folder of the running test, which contains its instance models.
     */
    private Path testFolder() {
        return Path.of(toFilePath("/collector/instance/" + testName));
    }

    private String toFilePath(String classPathResource) {
        try {
            return Path.of(Objects.requireNonNull(getClass().getResource(classPathResource)).toURI()).toString();
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

}
