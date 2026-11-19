package tools.vitruv.neojoin.collector;

import org.assertj.core.groups.Tuple;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EcorePackage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.eclipse.xtext.diagnostics.Severity.WARNING;

class PackageModelCollectorTest {

    /**
     * A directory entry whose name ends with the model extension (EMF's own jar has
     * {@code META-INF/maven/org.eclipse.emf/org.eclipse.emf.ecore/}) is not a model.
     */
    @Test
    void ignoresDirectoryEntriesOfArchives(@TempDir Path dir) throws IOException {
        var jar = dir.resolve("models.jar");
        try (var out = new ZipOutputStream(Files.newOutputStream(jar));
             InputStream model = Objects.requireNonNull(getClass().getResourceAsStream("/models/restaurant.ecore"))) {
            out.putNextEntry(new ZipEntry("META-INF/maven/org.example/org.example.ecore/"));
            out.closeEntry();
            out.putNextEntry(new ZipEntry("model/restaurant.ecore"));
            model.transferTo(out);
            out.closeEntry();
        }

        var result = new PackageModelCollector(jar.toString()).collect();

        assertThat(result.left()).isEmpty();
        assertThat(result.right().keySet()).containsExactly("http://example.org/restaurant");
    }

    /**
     * Collecting must not keep the archive open (on Windows an open archive cannot be deleted or replaced).
     */
    @Test
    void releasesArchivesAfterCollecting(@TempDir Path dir) throws IOException {
        var jar = dir.resolve("models.jar");
        try (var out = new ZipOutputStream(Files.newOutputStream(jar));
             InputStream model = Objects.requireNonNull(getClass().getResourceAsStream("/models/restaurant.ecore"))) {
            out.putNextEntry(new ZipEntry("model/restaurant.ecore"));
            model.transferTo(out);
            out.closeEntry();
        }

        new PackageModelCollector(jar.toString()).collect();

        Files.delete(jar);
        assertThat(jar).doesNotExist();
    }

    /**
     * Meta-models reference classes of other meta-models by their namespace URI, which is not in the global
     * package registry.
     */
    @Test
    void resolvesNamespaceUriReferencesBetweenCollectedPackages() {
        var searchPath = searchPath("/models/order.ecore", "/models/supplier-a.ecore", "/models/supplier-b.ecore");

        var result = new PackageModelCollector(searchPath).collect();

        assertThat(result.left()).isEmpty();
        var registry = result.right();
        var order = (EClass) registry.getEPackage("http://example.org/order").getEClassifier("Order");
        assertThat(order.getEStructuralFeature("fromA").getEType())
            .isSameAs(registry.getEPackage("http://example.org/supplier/a").getEClassifier("Food"));
        assertThat(order.getEStructuralFeature("fromB").getEType())
            .isSameAs(registry.getEPackage("http://example.org/supplier/b").getEClassifier("Food"));
    }

    /**
     * The source types of a query are inferred from all collected meta-models, which fails for a type that is not
     * one of them (see {@code SourceModelInferrer#getReferenceType}).
     */
    @Test
    void ignoresMetaModelReferencingClassOutsideModelPath() {
        var searchPath = searchPath("/models/order.ecore", "/models/supplier-a.ecore");

        var result = new PackageModelCollector(searchPath).collect();

        assertThat(result.left()).extracting(CollectorIssue::severity, Object::toString).containsExactly(
            warning("Ignoring the meta-model 'http://example.org/order': it references "
                + "'http://example.org/supplier/b#//Food', which is not on the meta-model path.")
        );
        assertThat(result.right().keySet()).containsExactly("http://example.org/supplier/a");
    }

    /**
     * The invoice meta-model references the payment meta-model, which references the order meta-model; its first
     * reference is to its own class.
     */
    @Test
    void ignoresMetaModelsReferencingIgnoredMetaModels(@TempDir Path dir) throws IOException {
        var payment = dir.resolve("payment.ecore");
        Files.writeString(payment, """
            <?xml version="1.0" encoding="UTF-8"?>
            <ecore:EPackage xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:ecore="http://www.eclipse.org/emf/2002/Ecore"
                name="payment" nsURI="http://example.org/payment" nsPrefix="payment">
              <eClassifiers xsi:type="ecore:EClass" name="Payment">
                <eStructuralFeatures xsi:type="ecore:EReference" name="order"
                    eType="ecore:EClass http://example.org/order#//Order"/>
              </eClassifiers>
            </ecore:EPackage>
            """);
        var invoice = dir.resolve("invoice.ecore");
        Files.writeString(invoice, """
            <?xml version="1.0" encoding="UTF-8"?>
            <ecore:EPackage xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:ecore="http://www.eclipse.org/emf/2002/Ecore"
                name="invoice" nsURI="http://example.org/invoice" nsPrefix="invoice">
              <eClassifiers xsi:type="ecore:EClass" name="Invoice">
                <eStructuralFeatures xsi:type="ecore:EReference" name="previous" eType="#//Invoice"/>
                <eStructuralFeatures xsi:type="ecore:EReference" name="payment"
                    eType="ecore:EClass http://example.org/payment#//Payment"/>
              </eClassifiers>
            </ecore:EPackage>
            """);
        var searchPath = searchPath("/models/order.ecore", "/models/supplier-a.ecore") + ";" + payment + ";" + invoice;

        var result = new PackageModelCollector(searchPath).collect();

        assertThat(result.left()).extracting(CollectorIssue::severity, Object::toString).containsExactly(
            warning("Ignoring the meta-model 'http://example.org/order': it references "
                + "'http://example.org/supplier/b#//Food', which is not on the meta-model path."),
            warning("Ignoring the meta-model 'http://example.org/payment': it references the ignored meta-model "
                + "'http://example.org/order'."),
            warning("Ignoring the meta-model 'http://example.org/invoice': it references the ignored meta-model "
                + "'http://example.org/payment'.")
        );
        assertThat(result.right().keySet()).containsExactly("http://example.org/supplier/a");
    }

    /**
     * The data type of the price is in XMLType, which is only in the global registry; the one of the cost is in an
     * unknown meta-model.
     */
    @Test
    void ignoresMetaModelsWithDataTypesOutsideModelPath(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("cost.ecore"), """
            <?xml version="1.0" encoding="UTF-8"?>
            <ecore:EPackage xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:ecore="http://www.eclipse.org/emf/2002/Ecore"
                name="cost" nsURI="http://example.org/cost" nsPrefix="cost">
              <eClassifiers xsi:type="ecore:EClass" name="Cost">
                <eStructuralFeatures xsi:type="ecore:EAttribute" name="amount"
                    eType="ecore:EDataType http://example.org/missing#//Money"/>
              </eClassifiers>
            </ecore:EPackage>
            """);
        Files.writeString(dir.resolve("price.ecore"), """
            <?xml version="1.0" encoding="UTF-8"?>
            <ecore:EPackage xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:ecore="http://www.eclipse.org/emf/2002/Ecore"
                name="price" nsURI="http://example.org/price" nsPrefix="price">
              <eClassifiers xsi:type="ecore:EClass" name="Price">
                <eStructuralFeatures xsi:type="ecore:EAttribute" name="amount"
                    eType="ecore:EDataType http://www.eclipse.org/emf/2003/XMLType#//Decimal"/>
              </eClassifiers>
            </ecore:EPackage>
            """);

        var result = new PackageModelCollector(dir.toString()).collect();

        assertThat(result.left()).extracting(CollectorIssue::severity, Object::toString).containsExactly(
            warning("Ignoring the meta-model 'http://example.org/cost': it references "
                + "'http://example.org/missing#//Money', which is not on the meta-model path."),
            warning("Ignoring the meta-model 'http://example.org/price': it references "
                + "'http://www.eclipse.org/emf/2003/XMLType#//Decimal', which is not on the meta-model path.")
        );
        assertThat(result.right().keySet()).isEmpty();
    }

    /**
     * A copy of Ecore on the path (for example from EMF's jar) must not replace Ecore: queries over Ecore models use
     * EMF's Ecore, with which the instance models are loaded.
     */
    @Test
    void usesGlobalPackageInsteadOfMetaModelWithItsNamespaceUri(@TempDir Path dir) throws IOException {
        var copy = dir.resolve("Ecore.ecore");
        copyResource("/model/Ecore.ecore", copy);
        copyResource("/models/eobject-ref.ecore", dir.resolve("eobject-ref.ecore"));

        var result = new PackageModelCollector(dir.toString()).collect();

        assertThat(result.left()).extracting(CollectorIssue::severity, Object::toString).containsExactly(
            warning("Ignoring the meta-model 'http://www.eclipse.org/emf/2002/Ecore' in '" + copy.toUri() + "': a "
                + "package with this namespace URI is registered globally and used instead.")
        );
        assertThat(result.right().getEPackage("http://www.eclipse.org/emf/2002/Ecore"))
            .isSameAs(EcorePackage.eINSTANCE);
        var holder = (EClass) result.right().getEPackage("http://vitruv.tools/eobjectref").getEClassifier("Holder");
        assertThat(holder.getEStructuralFeature("target").getEType()).isSameAs(EcorePackage.Literals.EOBJECT);
    }

    /**
     * Which of the meta-models with the same namespace URI is meant is unknown; the order meta-model references it.
     */
    @Test
    void ignoresMetaModelsWithSameNamespaceUri(@TempDir Path dir) throws IOException {
        var first = dir.resolve("a.ecore");
        Files.copy(Path.of(toFilePath("/models/supplier-a.ecore")), first);
        var second = dir.resolve("b.ecore");
        Files.copy(Path.of(toFilePath("/models/supplier-a.ecore")), second);
        Files.copy(Path.of(toFilePath("/models/order.ecore")), dir.resolve("order.ecore"));
        Files.copy(Path.of(toFilePath("/models/supplier-b.ecore")), dir.resolve("supplier-b.ecore"));

        var result = new PackageModelCollector(dir.toString()).collect();

        assertThat(result.left()).extracting(CollectorIssue::severity, Object::toString).containsExactly(
            warning("Ignoring the meta-models with the namespace URI 'http://example.org/supplier/a': several files "
                + "define it ('" + first.toUri() + "', '" + second.toUri() + "')."),
            warning("Ignoring the meta-model 'http://example.org/order': it references the ignored meta-model "
                + "'http://example.org/supplier/a'.")
        );
        assertThat(result.right().keySet()).containsExactly("http://example.org/supplier/b");
    }

    @Test
    void ignoresMetaModelFilesWithoutPackage(@TempDir Path dir) throws IOException {
        var empty = dir.resolve("empty.ecore");
        Files.writeString(empty, """
            <?xml version="1.0" encoding="UTF-8"?>
            <xmi:XMI xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI"/>
            """);
        var lonely = dir.resolve("lonely.ecore");
        Files.writeString(lonely, """
            <?xml version="1.0" encoding="UTF-8"?>
            <ecore:EClass xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI"
                xmlns:ecore="http://www.eclipse.org/emf/2002/Ecore" name="Lonely"/>
            """);

        var result = new PackageModelCollector(dir.toString()).collect();

        assertThat(result.left()).extracting(CollectorIssue::severity, Object::toString).containsExactly(
            warning("Cannot load '" + empty.toUri() + "': the file contains no package."),
            warning("Cannot load '" + lonely.toUri() + "': the file contains no package.")
        );
        assertThat(result.right().keySet()).isEmpty();
    }

    @Test
    void collectsPackagesOfAllRootsOfFile(@TempDir Path dir) throws IOException {
        var both = dir.resolve("both.ecore");
        Files.writeString(both, """
            <?xml version="1.0" encoding="UTF-8"?>
            <xmi:XMI xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI"
                xmlns:ecore="http://www.eclipse.org/emf/2002/Ecore">
              <ecore:EPackage name="first" nsURI="http://example.org/first" nsPrefix="first"/>
              <ecore:EPackage name="second" nsURI="http://example.org/second" nsPrefix="second"/>
            </xmi:XMI>
            """);

        var result = new PackageModelCollector(both.toString()).collect();

        assertThat(result.left()).isEmpty();
        assertThat(result.right().keySet())
            .containsExactlyInAnyOrder("http://example.org/first", "http://example.org/second");
    }

    @Test
    void ignoresArchiveThatCannotBeOpened(@TempDir Path dir) throws IOException {
        var broken = dir.resolve("broken.jar");
        Files.writeString(broken, "not an archive");
        Files.copy(Path.of(toFilePath("/models/supplier-a.ecore")), dir.resolve("supplier-a.ecore"));

        var result = new PackageModelCollector(dir.toString()).collect();

        assertThat(result.left()).singleElement().satisfies(issue -> {
            assertThat(issue.severity()).isEqualTo(WARNING);
            assertThat(issue.toString()).startsWith("Cannot load '" + broken.toUri() + "': ");
        });
        assertThat(result.right().keySet()).containsExactly("http://example.org/supplier/a");
    }

    /**
     * A NUL character is invalid in paths on all platforms; the reason in the message depends on the platform.
     */
    @Test
    void ignoresInvalidPathEntry() {
        var result = new PackageModelCollector(searchPath("/models/supplier-a.ecore") + ";a\u0000b").collect();

        assertThat(result.left()).singleElement().satisfies(issue -> {
            assertThat(issue.severity()).isEqualTo(WARNING);
            assertThat(issue.toString()).startsWith("Cannot load 'a\u0000b': the path is invalid (");
        });
        assertThat(result.right().keySet()).containsExactly("http://example.org/supplier/a");
    }

    /**
     * A relative reference loads the referenced file, although it is not on the meta-model path.
     */
    @Test
    void ignoresMetaModelWithSuperClassFromFileOutsideModelPath(@TempDir Path dir) throws IOException {
        Files.copy(Path.of(toFilePath("/models/supplier-a.ecore")), dir.resolve("supplier-a.ecore"));
        var special = dir.resolve("special.ecore");
        Files.writeString(special, """
            <?xml version="1.0" encoding="UTF-8"?>
            <ecore:EPackage xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:ecore="http://www.eclipse.org/emf/2002/Ecore"
                name="special" nsURI="http://example.org/special" nsPrefix="special">
              <eClassifiers xsi:type="ecore:EClass" name="SpecialFood" eSuperTypes="supplier-a.ecore#//Food"/>
            </ecore:EPackage>
            """);

        var result = new PackageModelCollector(special.toString()).collect();

        assertThat(result.left()).extracting(CollectorIssue::severity, Object::toString).containsExactly(
            warning("Ignoring the meta-model 'http://example.org/special': it references "
                + "'http://example.org/supplier/a#//Food', which is not on the meta-model path.")
        );
        assertThat(result.right().keySet()).isEmpty();
    }

    @Test
    void ignoresMetaModelWithDataTypeWithoutInstanceClass(@TempDir Path dir) throws IOException {
        var money = dir.resolve("money.ecore");
        Files.writeString(money, """
            <?xml version="1.0" encoding="UTF-8"?>
            <ecore:EPackage xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:ecore="http://www.eclipse.org/emf/2002/Ecore"
                name="money" nsURI="http://example.org/money" nsPrefix="money">
              <eClassifiers xsi:type="ecore:EClass" name="Price">
                <eStructuralFeatures xsi:type="ecore:EAttribute" name="amount" eType="#//Money"/>
              </eClassifiers>
              <eClassifiers xsi:type="ecore:EDataType" name="Money"/>
            </ecore:EPackage>
            """);

        var result = new PackageModelCollector(money.toString()).collect();

        assertThat(result.left()).extracting(CollectorIssue::severity, Object::toString).containsExactly(
            warning("Ignoring the meta-model 'http://example.org/money': the data type "
                + "'http://example.org/money#//Money' has no instance class.")
        );
        assertThat(result.right().keySet()).isEmpty();
    }

    /**
     * Ecore editors save a feature without type, which is an invalid meta-model.
     */
    @Test
    void ignoresMetaModelWithFeatureWithoutType(@TempDir Path dir) throws IOException {
        var untyped = dir.resolve("untyped.ecore");
        Files.writeString(untyped, """
            <?xml version="1.0" encoding="UTF-8"?>
            <ecore:EPackage xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:ecore="http://www.eclipse.org/emf/2002/Ecore"
                name="untyped" nsURI="http://example.org/untyped" nsPrefix="untyped">
              <eClassifiers xsi:type="ecore:EClass" name="Thing">
                <eStructuralFeatures xsi:type="ecore:EAttribute" name="value"/>
              </eClassifiers>
            </ecore:EPackage>
            """);

        var result = new PackageModelCollector(untyped.toString()).collect();

        assertThat(result.left()).extracting(CollectorIssue::severity, Object::toString).containsExactly(
            warning("Ignoring the meta-model 'http://example.org/untyped': the feature 'Thing.value' has no type.")
        );
        assertThat(result.right().keySet()).isEmpty();
    }

    @Test
    void ignoresMetaModelFileThatCannotBeLoaded(@TempDir Path dir) throws IOException {
        var broken = dir.resolve("broken.ecore");
        Files.writeString(broken, "<?xml version=\"1.0\"?>\n<ecore:EPackage");

        var result = new PackageModelCollector(searchPath("/models/supplier-a.ecore") + ";" + broken).collect();

        assertThat(result.left()).singleElement().satisfies(issue -> {
            assertThat(issue.severity()).isEqualTo(WARNING);
            assertThat(issue.toString()).startsWith("Cannot load '" + broken.toUri() + "': ");
        });
        assertThat(result.right().keySet()).containsExactly("http://example.org/supplier/a");
    }

    @Test
    void ignoresMissingPathEntry(@TempDir Path dir) {
        var missing = dir.resolve("missing");

        var result = new PackageModelCollector(searchPath("/models/supplier-a.ecore") + ";" + missing).collect();

        assertThat(result.left()).extracting(CollectorIssue::severity, Object::toString).containsExactly(
            warning("Cannot load '" + missing + "': the file or directory does not exist.")
        );
        assertThat(result.right().keySet()).containsExactly("http://example.org/supplier/a");
    }

    /**
     * Path entries can overlap, for example a directory and a file in it; a file found twice is not a second
     * meta-model with the same namespace URI.
     */
    @Test
    void collectsFileOnOverlappingPathEntriesOnce(@TempDir Path dir) throws IOException {
        var supplier = dir.resolve("supplier-a.ecore");
        copyResource("/models/supplier-a.ecore", supplier);

        var result = new PackageModelCollector(dir + ";" + supplier).collect();

        assertThat(result.left()).isEmpty();
        assertThat(result.right().keySet()).containsExactly("http://example.org/supplier/a");
    }

    private static Tuple warning(String message) {
        return tuple(WARNING, message);
    }

    /**
     * A path entry with a {@code .} segment ({@code -m .}) gives file URIs with that segment, while EMF resolves a
     * relative reference to a URI without it, which would load the referenced file a second time.
     */
    @Test
    void resolvesRelativeReferencesInPathEntryWithDotSegment(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("base.ecore"), """
            <?xml version="1.0" encoding="UTF-8"?>
            <ecore:EPackage xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:ecore="http://www.eclipse.org/emf/2002/Ecore"
                name="base" nsURI="http://example.org/base" nsPrefix="base">
              <eClassifiers xsi:type="ecore:EClass" name="Base"/>
            </ecore:EPackage>
            """);
        Files.writeString(dir.resolve("special.ecore"), """
            <?xml version="1.0" encoding="UTF-8"?>
            <ecore:EPackage xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:ecore="http://www.eclipse.org/emf/2002/Ecore"
                name="special" nsURI="http://example.org/special" nsPrefix="special">
              <eClassifiers xsi:type="ecore:EClass" name="Special" eSuperTypes="base.ecore#//Base"/>
            </ecore:EPackage>
            """);

        var result = new PackageModelCollector(dir.resolve(".").toString()).collect();

        assertThat(result.left()).isEmpty();
        assertThat(result.right().keySet())
            .containsExactlyInAnyOrder("http://example.org/base", "http://example.org/special");
    }

    /**
     * A relative reference can point to a class that is the root of its file and belongs to no package.
     */
    @Test
    void ignoresMetaModelWithSuperClassOutsideAnyPackage(@TempDir Path dir) throws IOException {
        var lonely = dir.resolve("lonely.ecore");
        Files.writeString(lonely, """
            <?xml version="1.0" encoding="UTF-8"?>
            <ecore:EClass xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI"
                xmlns:ecore="http://www.eclipse.org/emf/2002/Ecore" name="Lonely"/>
            """);
        var special = dir.resolve("special.ecore");
        Files.writeString(special, """
            <?xml version="1.0" encoding="UTF-8"?>
            <ecore:EPackage xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:ecore="http://www.eclipse.org/emf/2002/Ecore"
                name="special" nsURI="http://example.org/special" nsPrefix="special">
              <eClassifiers xsi:type="ecore:EClass" name="Special" eSuperTypes="lonely.ecore#/"/>
            </ecore:EPackage>
            """);

        var result = new PackageModelCollector(special.toString()).collect();

        assertThat(result.left()).extracting(CollectorIssue::severity, Object::toString).containsExactly(
            warning("Ignoring the meta-model 'http://example.org/special': it references '" + lonely.toUri() + "#/', "
                + "which is not on the meta-model path.")
        );
        assertThat(result.right().keySet()).isEmpty();
    }

    private void copyResource(String classPathResource, Path target) throws IOException {
        try (InputStream model = Objects.requireNonNull(getClass().getResourceAsStream(classPathResource))) {
            Files.copy(model, target);
        }
    }

    private String searchPath(String... classPathResources) {
        return Stream.of(classPathResources)
            .map(this::toFilePath)
            .collect(Collectors.joining(";"));
    }

    private String toFilePath(String classPathResource) {
        try {
            return Path.of(Objects.requireNonNull(getClass().getResource(classPathResource)).toURI()).toString();
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

}
