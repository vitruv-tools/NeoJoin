package tools.vitruv.neojoin;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.util.ITextRegionWithLineInformation;
import org.eclipse.xtext.util.LineAndColumn;
import org.eclipse.xtext.validation.Issue;

import java.nio.file.Path;

/**
 * Represents a text range within a file.
 *
 * @param file   file
 * @param start  start of the range
 * @param end    end of the range
 * @param length length of the range
 */
public record SourceLocation(
    URI file,
    LineAndColumn start,
    LineAndColumn end,
    int length
) {

    public static SourceLocation unknown(URI file) {
        return new SourceLocation(
            file,
            LineAndColumn.from(1, 1),
            LineAndColumn.from(1, 1),
            0
        );
    }

    public static SourceLocation from(EObject obj) {
        var file = EcoreUtil.getURI(obj).trimFragment();
        var node = NodeModelUtils.getNode(obj);
        if (node != null) {
            ITextRegionWithLineInformation nodeRegion = node.getTextRegionWithLineInformation();
            int offset = nodeRegion.getOffset();
            int length = nodeRegion.getLength();
            return new SourceLocation(
                file,
                NodeModelUtils.getLineAndColumn(node, offset),
                NodeModelUtils.getLineAndColumn(node, offset + length),
                length
            );
        }

        return SourceLocation.unknown(file);
    }

    public static SourceLocation from(Issue issue) {
        return new SourceLocation(
            issue.getUriToProblem().trimFragment(),
            LineAndColumn.from(issue.getLineNumber(), issue.getColumn()),
            LineAndColumn.from(issue.getLineNumberEnd(), issue.getColumnEnd()),
            issue.getLength()
        );
    }

    public String display() {
        return "%s:%d:%d".formatted(
            formatIssueFile(file()),
            start().getLine(),
            start().getColumn()
        );
    }

    /**
     * Formats the file as a {@code file:///path} URI (as {@link Path#toUri()} does) instead of the {@link URI EMF URI}
     * form {@code file:/path}. IntelliJ shows the first form followed by {@code :line:column} as a link to the
     * location. A relative path is resolved against the working directory; other URIs stay as they are.
     */
    private static String formatIssueFile(URI uri) {
        var file = uri.trimFragment();
        if (!file.isFile()) {
            return file.toString();
        }
        try {
            // an absolute file URI is converted by Java (handles drive letters and percent-encoding on all platforms)
            var path = file.isRelative() ? Path.of(file.toFileString()) : Path.of(java.net.URI.create(file.toString()));
            return path.toAbsolutePath().normalize().toUri().toString();
        } catch (IllegalArgumentException e) { // also InvalidPathException
            return file.toString();
        }
    }

}
