package tools.vitruv.neojoin.ide.custom;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class EcoreElementLocatorTest {

    @TempDir
    Path directory;

    private static String resource(String name) throws IOException {
        try (var stream = EcoreElementLocatorTest.class.getResourceAsStream("/ecore-element-locator/" + name)) {
            return new String(Objects.requireNonNull(stream).readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private EObject loadFirstRoot(String file, String text) throws IOException {
        var path = directory.resolve(file);
        Files.writeString(path, text);
        var resourceSet = new ResourceSetImpl();
        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("ecore", new XMIResourceFactoryImpl());
        return resourceSet.getResource(URI.createURI(path.toUri().toString()), true).getContents().get(0);
    }

    /**
     * Returns the range of the start tag of the element that starts in the given line (0-based) and ends in it.
     */
    private static Range rangeOfStartTag(String text, int line) {
        var content = text.lines().toList().get(line);
        return new Range(new Position(line, content.indexOf('<')), new Position(line, content.indexOf('>') + 1));
    }

    /**
     * The lookup follows the position of the element in the file, not its name.
     */
    @Test
    void featureWithSameNameInSeveralClassesIsFoundByItsPosition() throws IOException {
        var text = resource("same-named-features.ecore");
        var pack = (EPackage) loadFirstRoot("p.ecore", text);
        var feature = ((EClass) pack.getEClassifier("B")).getEStructuralFeature("name");

        var location = EcoreElementLocator.locate(feature);

        assertThat(location).isNotNull();
        assertThat(location.getUri()).isEqualTo(directory.resolve("p.ecore").toUri().toString());
        assertThat(location.getRange()).isEqualTo(rangeOfStartTag(text, 7));
    }

    @Test
    void classInNestedPackageIsFound() throws IOException {
        var text = resource("nested-package.ecore");
        var pack = (EPackage) loadFirstRoot("outer.ecore", text);

        var location = EcoreElementLocator.locate(pack.getESubpackages().get(0).getEClassifier("C"));

        assertThat(location).isNotNull();
        assertThat(location.getRange()).isEqualTo(rangeOfStartTag(text, 3));
    }

    /**
     * Files checked out on Windows have CRLF line breaks (the test resource itself has them there, too).
     */
    @Test
    void classInFileWithWindowsLineBreaksIsFound() throws IOException {
        var text = resource("two-classes.ecore").replace("\r\n", "\n").replace("\n", "\r\n");
        var pack = (EPackage) loadFirstRoot("crlf.ecore", text);

        var location = EcoreElementLocator.locate(pack.getEClassifier("B"));

        assertThat(location).isNotNull();
        assertThat(location.getRange()).isEqualTo(rangeOfStartTag(text, 3));
    }

    @Test
    void classInFileWithTabsAndNonAsciiCharactersIsFound() throws IOException {
        var text = resource("tabs-and-umlauts.ecore");
        var pack = (EPackage) loadFirstRoot("tabs.ecore", text);

        var location = EcoreElementLocator.locate(pack.getEClassifier("Birne"));

        assertThat(location).isNotNull();
        assertThat(location.getRange()).isEqualTo(rangeOfStartTag(text, 3));
    }

    /**
     * Editors do not count the byte order mark that some Windows editors add.
     */
    @Test
    void classInFileWithByteOrderMarkIsFound() throws IOException {
        var text = resource("two-classes.ecore");
        var pack = (EPackage) loadFirstRoot("bom.ecore", "\uFEFF" + text);

        var location = EcoreElementLocator.locate(pack.getEClassifier("B"));

        assertThat(location).isNotNull();
        assertThat(location.getRange()).isEqualTo(rangeOfStartTag(text, 3));
    }

    /**
     * The server loads the meta-models once: if the file changes afterwards, the position can belong to another
     * element.
     */
    @Test
    void elementThatDiffersFromTheFileHasNoLocation() throws IOException {
        var pack = (EPackage) loadFirstRoot("changed.ecore", resource("two-classes.ecore"));
        Files.writeString(directory.resolve("changed.ecore"), resource("two-classes.ecore").replace("\"B\"", "\"C\""));

        assertThat(EcoreElementLocator.locate(pack.getEClassifier("B"))).isNull();
    }

    @Test
    void elementOfFileWithSeveralRootsHasNoLocation() throws IOException {
        var text = resource("several-roots.ecore");

        assertThat(EcoreElementLocator.locate((EPackage) loadFirstRoot("roots.ecore", text))).isNull();
    }

    /**
     * Registered packages (for example Ecore itself) have no file that an editor could open.
     */
    @Test
    void elementOfRegisteredPackageHasNoLocation() {
        assertThat(EcoreElementLocator.locate(EcorePackage.Literals.EOBJECT)).isNull();
    }

}
