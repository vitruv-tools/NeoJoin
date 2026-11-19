package tools.vitruv.neojoin.collector;

import org.assertj.core.groups.Tuple;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EcorePackage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
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
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class PackageModelCollectorTest {

    private String testName;

    @BeforeEach
    void rememberTestName(TestInfo testInfo) {
        testName = testInfo.getTestMethod().orElseThrow().getName();
    }

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
    void ignoresMetaModelsReferencingIgnoredMetaModels() {
        var payment = testFolder().resolve("payment.ecore");
        var invoice = testFolder().resolve("invoice.ecore");
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
    void ignoresMetaModelsWithDataTypesOutsideModelPath() {
        var result = new PackageModelCollector(testFolder().toString()).collect();

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
    void usesGlobalPackageInsteadOfMetaModelWithItsNamespaceUri() {
        var copy = testFolder().resolve("Ecore.ecore");

        var result = new PackageModelCollector(testFolder() + ";" + searchPath("/models/eobject-ref.ecore")).collect();

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
    void ignoresMetaModelsWithSameNamespaceUri() {
        var first = testFolder().resolve("a.ecore");
        var second = testFolder().resolve("b.ecore");
        var searchPath = testFolder() + ";" + searchPath("/models/order.ecore", "/models/supplier-b.ecore");

        var result = new PackageModelCollector(searchPath).collect();

        assertThat(result.left()).extracting(CollectorIssue::severity, Object::toString).containsExactly(
            warning("Ignoring the meta-models with the namespace URI 'http://example.org/supplier/a': several files "
                + "define it ('" + first.toUri() + "', '" + second.toUri() + "')."),
            warning("Ignoring the meta-model 'http://example.org/order': it references the ignored meta-model "
                + "'http://example.org/supplier/a'.")
        );
        assertThat(result.right().keySet()).containsExactly("http://example.org/supplier/b");
    }

    @Test
    void ignoresMetaModelFilesWithoutPackage() {
        var empty = testFolder().resolve("empty.ecore");
        var lonely = testFolder().resolve("lonely.ecore");

        var result = new PackageModelCollector(testFolder().toString()).collect();

        assertThat(result.left()).extracting(CollectorIssue::severity, Object::toString).containsExactly(
            warning("Cannot load '" + empty.toUri() + "': the file contains no package."),
            warning("Cannot load '" + lonely.toUri() + "': the file contains no package.")
        );
        assertThat(result.right().keySet()).isEmpty();
    }

    @Test
    void collectsPackagesOfAllRootsOfFile() {
        var result = new PackageModelCollector(testFolder().resolve("both.ecore").toString()).collect();

        assertThat(result.left()).isEmpty();
        assertThat(result.right().keySet())
            .containsExactlyInAnyOrder("http://example.org/first", "http://example.org/second");
    }

    /**
     * The archive is a text file.
     */
    @Test
    void ignoresArchiveThatCannotBeOpened() {
        var broken = testFolder().resolve("broken.jar");

        var result = new PackageModelCollector(testFolder() + ";" + searchPath("/models/supplier-a.ecore")).collect();

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
            assertThat(issue.toString())
                .startsWith("Ignoring 'a\u0000b' on the meta-model path: the path is invalid (");
        });
        assertThat(result.right().keySet()).containsExactly("http://example.org/supplier/a");
    }

    /**
     * A relative reference loads the referenced file, although it is not on the meta-model path.
     */
    @Test
    void ignoresMetaModelWithSuperClassFromFileOutsideModelPath() {
        var result = new PackageModelCollector(testFolder().resolve("special.ecore").toString()).collect();

        assertThat(result.left()).extracting(CollectorIssue::severity, Object::toString).containsExactly(
            warning("Ignoring the meta-model 'http://example.org/special': it references "
                + "'http://example.org/supplier/a#//Food', which is not on the meta-model path.")
        );
        assertThat(result.right().keySet()).isEmpty();
    }

    @Test
    void ignoresMetaModelWithDataTypeWithoutInstanceClass() {
        var result = new PackageModelCollector(testFolder().resolve("money.ecore").toString()).collect();

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
    void ignoresMetaModelWithFeatureWithoutType() {
        var result = new PackageModelCollector(testFolder().resolve("untyped.ecore").toString()).collect();

        assertThat(result.left()).extracting(CollectorIssue::severity, Object::toString).containsExactly(
            warning("Ignoring the meta-model 'http://example.org/untyped': the feature 'Thing.value' has no type.")
        );
        assertThat(result.right().keySet()).isEmpty();
    }

    /**
     * The file ends within the start tag of the package.
     */
    @Test
    void ignoresMetaModelFileThatCannotBeLoaded() {
        var broken = testFolder().resolve("broken.ecore");

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
            warning("Ignoring '" + missing + "' on the meta-model path: the file or directory does not exist.")
        );
        assertThat(result.right().keySet()).containsExactly("http://example.org/supplier/a");
    }

    /**
     * Within a directory, files with other extensions are ignored silently, but a file named directly is meant as a
     * model.
     */
    @Test
    void ignoresPathEntryWithOtherFileExtension() {
        var instance = testFolder().resolve("restaurants.xmi");

        var result = new PackageModelCollector(searchPath("/models/supplier-a.ecore") + ";" + instance).collect();

        assertThat(result.left()).extracting(CollectorIssue::severity, Object::toString).containsExactly(
            warning("Ignoring '" + instance + "' on the meta-model path: the file does not have the extension "
                + "'.ecore'.")
        );
        assertThat(result.right().keySet()).containsExactly("http://example.org/supplier/a");
    }

    /**
     * Path entries can overlap, for example a directory and a file in it; a file found twice is not a second
     * meta-model with the same namespace URI.
     */
    @Test
    void collectsFileOnOverlappingPathEntriesOnce() {
        var supplier = testFolder().resolve("supplier-a.ecore");

        var result = new PackageModelCollector(testFolder() + ";" + supplier).collect();

        assertThat(result.left()).isEmpty();
        assertThat(result.right().keySet()).containsExactly("http://example.org/supplier/a");
    }

    /**
     * An empty entry (a leading or doubled separator) would be the working directory, which would be searched for
     * models; here the module directory, which contains the test models.
     */
    @Test
    void ignoresEmptyPathEntries() {
        var result = new PackageModelCollector(";" + searchPath("/models/supplier-a.ecore") + ";;").collect();

        assertThat(result.left()).isEmpty();
        assertThat(result.right().keySet()).containsExactly("http://example.org/supplier/a");
    }

    /**
     * Read permissions cannot be removed on every platform (Windows, or as root on Linux); there, the test is skipped.
     */
    @Test
    void ignoresOnlyTheDirectoryThatCannotBeRead(@TempDir Path dir) throws IOException {
        copyResource("/models/supplier-a.ecore", dir.resolve("supplier-a.ecore"));
        var locked = Files.createDirectory(dir.resolve("locked"));
        copyResource("/models/supplier-b.ecore", locked.resolve("supplier-b.ecore"));
        assumeTrue(locked.toFile().setReadable(false) && !Files.isReadable(locked));

        try {
            var result = new PackageModelCollector(dir.toString()).collect();

            assertThat(result.left()).extracting(CollectorIssue::severity, Object::toString).containsExactly(
                warning("Cannot load '" + locked.toUri() + "': access denied.")
            );
            assertThat(result.right().keySet()).containsExactly("http://example.org/supplier/a");
        } finally {
            locked.toFile().setReadable(true);
        }
    }

    /**
     * The relative reference points to the copy, which the global package replaces; a reference by namespace URI
     * would find the global package.
     */
    @Test
    void ignoresMetaModelReferencingReplacedCopyOfGlobalPackage() {
        var copy = testFolder().resolve("Ecore.ecore");

        var result = new PackageModelCollector(testFolder().toString()).collect();

        assertThat(result.left()).extracting(CollectorIssue::severity, Object::toString).containsExactly(
            warning("Ignoring the meta-model 'http://www.eclipse.org/emf/2002/Ecore' in '" + copy.toUri() + "': a "
                + "package with this namespace URI is registered globally and used instead."),
            warning("Ignoring the meta-model 'http://example.org/shop': it references '" + copy.toUri() + "#//EString' "
                + "in a copy of the globally registered meta-model 'http://www.eclipse.org/emf/2002/Ecore'; reference "
                + "it by namespace URI instead.")
        );
        assertThat(result.right().keySet()).containsExactly("http://www.eclipse.org/emf/2002/Ecore");
    }

    private static Tuple warning(String message) {
        return tuple(WARNING, message);
    }

    /**
     * A path entry with a {@code .} segment ({@code -m .}) gives file URIs with that segment, while EMF resolves a
     * relative reference to a URI without it, which would load the referenced file a second time.
     */
    @Test
    void resolvesRelativeReferencesInPathEntryWithDotSegment() {
        var result = new PackageModelCollector(testFolder().resolve(".").toString()).collect();

        assertThat(result.left()).isEmpty();
        assertThat(result.right().keySet())
            .containsExactlyInAnyOrder("http://example.org/base", "http://example.org/special");
    }

    /**
     * A relative reference can point to a class that is the root of its file and belongs to no package.
     */
    @Test
    void ignoresMetaModelWithSuperClassOutsideAnyPackage() {
        var lonely = testFolder().resolve("lonely.ecore");

        var result = new PackageModelCollector(testFolder().resolve("special.ecore").toString()).collect();

        assertThat(result.left()).extracting(CollectorIssue::severity, Object::toString).containsExactly(
            warning("Ignoring the meta-model 'http://example.org/special': it references '" + lonely.toUri() + "#/', "
                + "which is not on the meta-model path.")
        );
        assertThat(result.right().keySet()).isEmpty();
    }

    /**
     * Returns the resource folder of the running test, which contains its models.
     */
    private Path testFolder() {
        return Path.of(toFilePath("/collector/package/" + testName));
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
