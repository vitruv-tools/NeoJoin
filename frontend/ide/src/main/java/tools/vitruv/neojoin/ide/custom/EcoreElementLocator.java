package tools.vitruv.neojoin.ide.custom;

import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.jspecify.annotations.Nullable;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Finds the text range of the start tag of an element in its {@code .ecore} (XMI) file.
 * <p>
 * EMF does not record where it read an element, so this walks the containment path of the element through the XML:
 * each step is the containment feature of the element (the tag of its XML element) and the index of the element in it.
 */
public final class EcoreElementLocator {

    private EcoreElementLocator() {
    }

    private record Step(String tag, int index) {
    }

    /**
     * @param end  offset after the start tag
     * @param name value of the attribute {@code name}, {@code null} if the tag has none
     */
    private record StartTag(int end, @Nullable String name) {
    }

    /**
     * Returns the location of the element's start tag (from {@code <} to {@code >}), or {@code null} if there is none:
     * the element is not in a file (for example a registered package or a model in a jar), the file has several root
     * elements, the file cannot be read, or the file does not (any longer) contain the element at its position (the
     * server loads the meta-models once, so the file can be newer than the loaded element).
     */
    public static @Nullable Location locate(ENamedElement element) {
        var resource = element.eResource();
        if (resource == null || !resource.getURI().isFile() || resource.getContents().size() != 1) {
            return null;
        }
        try {
            // not toFileString(): it yields "//\C:\..." for file URIs with a Windows drive letter
            var file = Path.of(java.net.URI.create(resource.getURI().toString()));
            var text = withoutByteOrderMark(Files.readString(file));
            var tag = findStartTag(text, containmentPath(element));
            if (tag == null || !Objects.equals(element.getName(), tag.name())) {
                return null;
            }
            var range = new Range(position(text, text.lastIndexOf('<', tag.end() - 1)), position(text, tag.end()));
            return new Location(file.toUri().toString(), range);
        } catch (IOException | XMLStreamException | IllegalArgumentException | FileSystemNotFoundException e) {
            // a file that cannot be read or parsed (anymore) has no location; the exceptions of Path.of are for a
            // URI that is no valid path
            return null;
        }
    }

    /**
     * An editor does not count the byte order mark as a character of the first line.
     */
    private static String withoutByteOrderMark(String text) {
        return text.startsWith("\uFEFF") ? text.substring(1) : text;
    }

    private static List<Step> containmentPath(ENamedElement element) {
        var steps = new ArrayList<Step>();
        for (EObject object = element; object.eContainer() != null; object = object.eContainer()) {
            var feature = object.eContainmentFeature();
            int index = feature.isMany() ? ((List<?>) object.eContainer().eGet(feature)).indexOf(object) : 0;
            steps.add(0, new Step(feature.getName(), index));
        }
        return steps;
    }

    /**
     * Follows the XML events along a containment path.
     */
    private static final class PathMatcher {

        private final List<Step> path;
        private int depth; // depth of the current XML element, the root element has depth 1
        private int matched; // steps found so far: the XML element at depth matched + 1 is the last one found
        private int seen; // children found with the tag of the next step (the index counts from 0)

        PathMatcher(List<Step> path) {
            this.path = path;
        }

        /**
         * Returns whether the XML element that starts is the element at the end of the path.
         */
        boolean isEndOfPathAt(String tag) {
            depth++;
            if (path.isEmpty()) {
                return depth == 1;
            }
            var next = path.get(matched);
            if (depth != matched + 2 || !tag.equals(next.tag())) {
                return false;
            }
            if (seen < next.index()) {
                seen++;
                return false;
            }
            seen = 0;
            matched++;
            return matched == path.size();
        }

        /**
         * Returns whether the last element found is still open (otherwise, the next step is not in it).
         */
        boolean isLastFoundElementOpenAfterLeaving() {
            depth--;
            return depth >= matched + 1;
        }

    }

    /**
     * Returns the start tag of the XML element at the end of the path, or {@code null} if there is none.
     */
    private static @Nullable StartTag findStartTag(String text, List<Step> path) throws XMLStreamException {
        var factory = XMLInputFactory.newInstance();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        var reader = factory.createXMLStreamReader(new StringReader(text));
        try {
            var matcher = new PathMatcher(path);
            while (reader.hasNext()) {
                var event = reader.next();
                if (event == XMLStreamConstants.START_ELEMENT && matcher.isEndOfPathAt(reader.getLocalName())) {
                    return startTag(text, reader);
                }
                if (event == XMLStreamConstants.END_ELEMENT && !matcher.isLastFoundElementOpenAfterLeaving()) {
                    return null;
                }
            }
            return null;
        } finally {
            reader.close();
        }
    }

    /**
     * Returns the start tag that the reader has just read. Uses line and column of the location for the end of the
     * tag: the character offset of the StAX implementation in the JDK is unreliable (it was wrong for 887 of 1273
     * start tags in 89 {@code .ecore} files, line and column for none; JDK-8350217).
     */
    private static StartTag startTag(String text, XMLStreamReader reader) {
        var location = reader.getLocation();
        int lineStart = 0;
        for (int line = 1; line < location.getLineNumber(); line++) {
            lineStart = text.indexOf('\n', lineStart) + 1;
        }
        return new StartTag(lineStart + location.getColumnNumber() - 1, reader.getAttributeValue(null, "name"));
    }

    private static Position position(String text, int offset) {
        int line = 0;
        int lineStart = 0;
        for (int i = text.indexOf('\n'); i >= 0 && i < offset; i = text.indexOf('\n', i + 1)) {
            line++;
            lineStart = i + 1;
        }
        return new Position(line, offset - lineStart);
    }

}
