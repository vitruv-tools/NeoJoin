package tools.vitruv.neojoin.ide.integration;

import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

/**
 * Positions in the text of a query, found by searching for a piece of text (the last occurrence).
 *
 * @param text text of the query
 */
record QueryText(String text) {

    /**
     * Returns the position of the last character of the last occurrence of {@code part}.
     */
    Position positionAtEndOf(String part) {
        var lines = text.lines().toList();
        for (int line = lines.size() - 1; line >= 0; line--) {
            int column = lines.get(line).lastIndexOf(part);
            if (column >= 0) {
                return new Position(line, column + part.length() - 1);
            }
        }
        throw new IllegalArgumentException("Not in the query: " + part);
    }

    /**
     * Returns the position right after the last occurrence of {@code part}, where the cursor is when it touches the end
     * of the text.
     */
    Position positionAfter(String part) {
        var end = positionAtEndOf(part);
        return new Position(end.getLine(), end.getCharacter() + 1);
    }

    /**
     * Returns the range of {@code name} at the end of the last occurrence of {@code part}.
     */
    Range rangeAtEndOf(String part, String name) {
        var end = positionAtEndOf(part);
        return new Range(
            new Position(end.getLine(), end.getCharacter() + 1 - name.length()),
            new Position(end.getLine(), end.getCharacter() + 1)
        );
    }

    /**
     * Returns the range of the XML start tag (from {@code <} to {@code >}) that contains the last occurrence of
     * {@code part}.
     */
    Range rangeOfStartTagContaining(String part) {
        int offset = text.lastIndexOf(part);
        if (offset < 0) {
            throw new IllegalArgumentException("Not in the text: " + part);
        }
        return new Range(positionAt(text.lastIndexOf('<', offset)), positionAt(text.indexOf('>', offset) + 1));
    }

    private Position positionAt(int offset) {
        int line = 0;
        int lineStart = 0;
        for (int i = text.indexOf('\n'); i >= 0 && i < offset; i = text.indexOf('\n', i + 1)) {
            line++;
            lineStart = i + 1;
        }
        return new Position(line, offset - lineStart);
    }

    /**
     * Returns the range of the last character of the last occurrence of {@code part}.
     */
    Range rangeOfLastCharacterOf(String part) {
        return rangeAtEndOf(part, part.substring(part.length() - 1));
    }

}
