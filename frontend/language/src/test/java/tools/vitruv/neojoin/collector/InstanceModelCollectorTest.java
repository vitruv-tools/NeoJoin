package tools.vitruv.neojoin.collector;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcorePackage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.eclipse.xtext.diagnostics.Severity.ERROR;
import static org.eclipse.xtext.diagnostics.Severity.WARNING;

class InstanceModelCollectorTest {

    private static final String HOLDER = """
        <?xml version="1.0" encoding="UTF-8"?>
        <eobjectref:Holder xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI"
            xmlns:eobjectref="http://vitruv.tools/eobjectref" name="holder"/>
        """;

    @TempDir
    Path dir;

    private Path instances;
    private EPackage.Registry registry;

    @BeforeEach
    void collectMetaModel() throws IOException {
        var metaModel = dir.resolve("eobject-ref.ecore");
        try (InputStream model = Objects.requireNonNull(getClass().getResourceAsStream("/models/eobject-ref.ecore"))) {
            Files.copy(model, metaModel);
        }
        registry = new PackageModelCollector(metaModel.toString()).collect().right();
        instances = Files.createDirectory(dir.resolve("instances"));
    }

    /**
     * Source meta-models may reference Ecore classes, so their instances may reference elements of Ecore.
     */
    @Test
    void resolvesReferencesToEcoreElements() throws IOException {
        Files.writeString(instances.resolve("holder.xmi"), """
            <?xml version="1.0" encoding="UTF-8"?>
            <eobjectref:Holder xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:ecore="http://www.eclipse.org/emf/2002/Ecore"
                xmlns:eobjectref="http://vitruv.tools/eobjectref" name="holder">
              <target xsi:type="ecore:EClass" href="http://www.eclipse.org/emf/2002/Ecore#//EClass"/>
            </eobjectref:Holder>
            """);

        var result = new InstanceModelCollector(instances.toString(), registry).collect();

        assertThat(result.left()).isEmpty();
        var holder = result.right().get(registry.getEPackage("http://vitruv.tools/eobjectref")).getContents().get(0);
        assertThat(holder.eGet(holder.eClass().getEStructuralFeature("target"))).isSameAs(EcorePackage.Literals.ECLASS);
    }

    /**
     * The meta-model of the restaurants is unknown, the one of the package (Ecore) is only in the global registry:
     * queries cannot import either of them.
     */
    @Test
    void ignoresInstanceModelsOfMetaModelsOutsideModelPath() throws IOException {
        Files.writeString(instances.resolve("holder.xmi"), HOLDER);
        var restaurants = instances.resolve("restaurants.xmi");
        Files.writeString(restaurants, """
            <?xml version="1.0" encoding="UTF-8"?>
            <restaurant:Restaurant xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI"
                xmlns:restaurant="http://example.org/restaurant"/>
            """);
        var pack = instances.resolve("package.xmi");
        Files.writeString(pack, """
            <?xml version="1.0" encoding="UTF-8"?>
            <ecore:EPackage xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI"
                xmlns:ecore="http://www.eclipse.org/emf/2002/Ecore" name="example"/>
            """);

        var result = new InstanceModelCollector(instances.toString(), registry).collect();

        assertThat(result.left()).extracting(CollectorIssue::severity, Object::toString).containsExactly(
            tuple(WARNING, "Ignoring the instance model '" + pack.toUri() + "': it uses the meta-model "
                + "'http://www.eclipse.org/emf/2002/Ecore', which is not on the meta-model path."),
            tuple(WARNING, "Ignoring the instance model '" + restaurants.toUri() + "': it uses the meta-model "
                + "'http://example.org/restaurant', which is not on the meta-model path.")
        );
        assertThat(result.right()).containsOnlyKeys(registry.getEPackage("http://vitruv.tools/eobjectref"));
    }

    @Test
    void reportsMultipleInstanceModelsOfOneMetaModel() throws IOException {
        var first = instances.resolve("a.xmi");
        Files.writeString(first, HOLDER);
        var second = instances.resolve("b.xmi");
        Files.writeString(second, HOLDER);

        var result = new InstanceModelCollector(instances.toString(), registry).collect();

        assertThat(result.left()).extracting(CollectorIssue::severity, Object::toString).containsExactly(
            tuple(ERROR, "Found several instance models of the meta-model 'http://vitruv.tools/eobjectref': '"
                + first.toUri() + "' and '" + second.toUri() + "'.")
        );
    }

    /**
     * An instance model can contain the roots of several meta-models within {@code xmi:XMI}.
     */
    @Test
    void usesInstanceModelForTheMetaModelsOfAllRoots() throws IOException {
        var restaurantMetaModel = dir.resolve("restaurant.ecore");
        try (InputStream model = Objects.requireNonNull(getClass().getResourceAsStream("/models/restaurant.ecore"))) {
            Files.copy(model, restaurantMetaModel);
        }
        var bothRegistry = new PackageModelCollector(
            dir.resolve("eobject-ref.ecore") + ";" + restaurantMetaModel
        ).collect().right();
        Files.writeString(instances.resolve("both.xmi"), """
            <?xml version="1.0" encoding="UTF-8"?>
            <xmi:XMI xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI"
                xmlns:eobjectref="http://vitruv.tools/eobjectref" xmlns:restaurant="http://example.org/restaurant">
              <eobjectref:Holder name="holder"/>
              <restaurant:Restaurant/>
            </xmi:XMI>
            """);

        var result = new InstanceModelCollector(instances.toString(), bothRegistry).collect();

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
    void assignsInstanceModelOfSubPackageToItsRootPackage() throws IOException {
        var zooRegistry = collectZooMetaModel();
        Files.writeString(instances.resolve("animal.xmi"), """
            <?xml version="1.0" encoding="UTF-8"?>
            <animals:Animal xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI"
                xmlns:animals="http://example.org/zoo/animals"/>
            """);

        var result = new InstanceModelCollector(instances.toString(), zooRegistry).collect();

        assertThat(result.left()).isEmpty();
        assertThat(result.right()).containsOnlyKeys(zooRegistry.getEPackage("http://example.org/zoo"));
    }

    /**
     * The root element of the broken file is incomplete, so its meta-model is unknown.
     */
    @Test
    void ignoresInstanceModelsThatCannotBeLoaded() throws IOException {
        Files.writeString(instances.resolve("a.xmi"), HOLDER);
        var broken = instances.resolve("b.xmi");
        Files.writeString(broken, "<?xml version=\"1.0\"?>\n<eobjectref:Holder");
        var empty = instances.resolve("c.xmi");
        Files.writeString(empty, """
            <?xml version="1.0" encoding="UTF-8"?>
            <xmi:XMI xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI"/>
            """);

        var result = new InstanceModelCollector(instances.toString(), registry).collect();

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
    void reportsMultipleInstanceModelsOfOneMetaModelAlsoIfTheyCannotBeLoaded() throws IOException {
        var first = instances.resolve("a.xmi");
        Files.writeString(first, """
            <?xml version="1.0" encoding="UTF-8"?>
            <eobjectref:Container xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:eobjectref="http://vitruv.tools/eobjectref">
              <content xsi:type="unknown:Thing" xmlns:unknown="http://example.org/unknown"/>
            </eobjectref:Container>
            """);
        var second = instances.resolve("b.xmi");
        Files.writeString(second, """
            <?xml version="1.0" encoding="UTF-8"?>
            <xmi:XMI xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:eobjectref="http://vitruv.tools/eobjectref">
              <eobjectref:Holder name="second">
            </xmi:XMI>
            """);
        var third = instances.resolve("c.xmi");
        Files.writeString(third, HOLDER);

        var result = new InstanceModelCollector(instances.toString(), registry).collect();

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
    void reportsInstanceModelOfSubPackageThatCannotBeLoadedAsInstanceModelOfItsRootPackage() throws IOException {
        var zooRegistry = collectZooMetaModel();
        var zoo = instances.resolve("a.xmi");
        Files.writeString(zoo, """
            <?xml version="1.0" encoding="UTF-8"?>
            <zoo:Zoo xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:zoo="http://example.org/zoo"/>
            """);
        var animal = instances.resolve("b.xmi");
        Files.writeString(animal, """
            <?xml version="1.0" encoding="UTF-8"?>
            <animals:Animal xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI"
                xmlns:animals="http://example.org/zoo/animals">
            </animals:Animals>
            """);

        var result = new InstanceModelCollector(instances.toString(), zooRegistry).collect();

        assertThat(result.left()).filteredOn(issue -> issue.severity() == ERROR).map(Object::toString).containsExactly(
            "Found several instance models of the meta-model 'http://example.org/zoo': '" + zoo.toUri() + "' and '"
                + animal.toUri() + "'."
        );
    }

    private EPackage.Registry collectZooMetaModel() throws IOException {
        var metaModel = dir.resolve("zoo.ecore");
        Files.writeString(metaModel, """
            <?xml version="1.0" encoding="UTF-8"?>
            <ecore:EPackage xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:ecore="http://www.eclipse.org/emf/2002/Ecore"
                name="zoo" nsURI="http://example.org/zoo" nsPrefix="zoo">
              <eClassifiers xsi:type="ecore:EClass" name="Zoo"/>
              <eSubpackages name="animals" nsURI="http://example.org/zoo/animals" nsPrefix="animals">
                <eClassifiers xsi:type="ecore:EClass" name="Animal"/>
              </eSubpackages>
            </ecore:EPackage>
            """);
        return new PackageModelCollector(metaModel.toString()).collect().right();
    }

}
