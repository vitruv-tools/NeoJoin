package tools.vitruv.neojoin.ide.visualization;

import org.eclipse.emf.ecore.ETypedElement;

/**
 * This class can be used to build source code for PlantUML class diagrams. All PlantUML syntax lives here, the
 * callers decide what is drawn.
 */
public class PlantUMLBuilder {

    public static final Runnable Empty = () -> {};

    private static final String Header = """
        hide circle
        hide empty members
        !pragma useIntermediatePackages false
        <style>
            element {
                header {
                    FontStyle: bold
                }
            }
        </style>""".stripIndent();

    private static final String OrthogonalArrows = "skinparam linetype ortho";
    private static final String ReferenceNormal = "-->";
    private static final String ReferenceContainment = "*-->";
    private static final String ReferenceInheritanceUpwards = "-up-|>";
    private static final String ArrowSourceClass = ".down.>";

    private final StringBuilder out = new StringBuilder();

    public boolean isEmpty() {
        return out.isEmpty();
    }

    public String build() {
        return out.toString();
    }

    /**
     * Appends the content of another builder, for example to write the body of a diagram before its packages are known.
     */
    public void include(PlantUMLBuilder other) {
        appendln(other.build().stripTrailing());
    }

    private void appendln(String... text) {
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

    public void header(boolean orthogonalArrows) {
        appendln(Header);
        if (orthogonalArrows) {
            appendln(OrthogonalArrows);
        }
    }

    private void block(String prefix, Runnable content) {
        appendln(prefix, " {");
        content.run();
        appendln("}");
    }

    /**
     * Groups the packages in the content, so that PlantUML draws them next to each other.
     */
    public void together(Runnable content) {
        block("together", content);
    }

    /**
     * An empty package whose name is also its id.
     */
    public void sourcePackage(String name) {
        block("package " + name, Empty);
    }

    /**
     * An empty package with a title and an id that classes use as prefix of their names.
     */
    public void targetPackage(String title, String id) {
        block("package \"%s\" as %s".formatted(title, id), Empty);
    }

    /**
     * An empty package with the stereotype {@code external}.
     */
    public void externalPackage(String title, String id) {
        block("package \"%s\" as %s <<external>>".formatted(title, id), Empty);
    }

    public void enumeration(String name, Runnable content) {
        block("enum %s <<enum>>".formatted(name), content);
    }

    public void literal(String name) {
        appendln(name);
    }

    public void clazz(String name, boolean isAbstract, Runnable content) {
        block((isAbstract ? "abstract " : "") + "class " + name, content);
    }

    public void attribute(String name, String type) {
        appendln(name, " : ", type);
    }

    public void reference(String from, boolean isContainment, String to, String name, int lowerBound, int upperBound) {
        appendln(
            from, " ", isContainment ? ReferenceContainment : ReferenceNormal, " \"",
            formatArity(lowerBound, upperBound), "\" ", to, " : ", name
        );
    }

    public void inheritance(String from, String to) {
        appendln(from, " ", ReferenceInheritanceUpwards, " ", to);
    }

    public void sourceToTarget(String source, String target) {
        appendln(source, " ", ArrowSourceClass, " ", target);
    }

    private static String formatArity(int lowerBound, int upperBound) {
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
