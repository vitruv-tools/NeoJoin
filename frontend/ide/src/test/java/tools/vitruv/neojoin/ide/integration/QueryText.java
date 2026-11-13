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
     * Returns the range of the last character of the last occurrence of {@code part}.
     */
    Range rangeOfLastCharacterOf(String part) {
        return rangeAtEndOf(part, part.substring(part.length() - 1));
    }

}
