package tools.vitruv.neojoin.ide.visualization;

import org.eclipse.emf.ecore.ETypedElement;
import tools.vitruv.neojoin.utils.Pair;

/**
 * This class can be used to build source code for PlantUML class diagrams.
 */
public class PlantUMLBuilder {

    public static final Runnable Empty = () -> {};

    public static final String ReferenceNormal = "-->";
    public static final String ReferenceContainment = "*-->";
    public static final String ReferenceInheritance = "--|>";
    public static final String ReferenceInheritanceUpwards = "-up-|>";

    private final StringBuilder out = new StringBuilder();

    public boolean isEmpty() {
        return out.isEmpty();
    }

    public String build() {
        return out.toString();
    }

    public void appendln(String line) {
        out.append(line).append("\n");
    }

    public void appendln(String... text) {
        for (var t : text) {
            out.append(t);
        }
        out.append("\n");
    }

    public void document(Runnable content) {
        appendln("@startuml");
        content.run();
        appendln("@enduml");
    }

    public void block(String prefix, Runnable content) {
        appendln(prefix, " {");
        content.run();
        appendln("}");
    }

    public void pack(String name, Runnable content) {
        block("package " + name, content);
    }

    public void enumeration(String name, Runnable content) {
        block("enum %s <<enum>>".formatted(name), content);
    }

    public void literal(String name) {
        appendln(name);
    }

    public void clazz(String name, Runnable content) {
        block("class " + name, content);
    }

    public void attribute(String name, String type) {
        appendln(name, " : ", type);
    }

    public void reference(String from, String type, String to, String name, Pair<Integer, Integer> arity) {
        appendln(from, " ", type, " \"", formatArity(arity), "\" ", to, " : ", name);
    }

    public void arrow(String from, String type, String to) {
        appendln(from, " ", type, " ", to);
    }

    private static String formatArity(Pair<Integer, Integer> arity) {
        //noinspection DataFlowIssue - false positive
        int lowerBound = arity.left();
        //noinspection DataFlowIssue - false positive
        int upperBound = arity.right();
        if (lowerBound == upperBound) {
            return Integer.toString(lowerBound);
        } else if (lowerBound == 0 && upperBound == ETypedElement.UNBOUNDED_MULTIPLICITY) {
            return "*";
        } else {
            return lowerBound + ".." + formatUpperBound(upperBound);
        }
    }

    private static String formatUpperBound(int upperBound) {
        return switch (upperBound) {
            case ETypedElement.UNBOUNDED_MULTIPLICITY -> "*";
            case ETypedElement.UNSPECIFIED_MULTIPLICITY -> "?";
            default -> Integer.toString(upperBound);
        };
    }

}
