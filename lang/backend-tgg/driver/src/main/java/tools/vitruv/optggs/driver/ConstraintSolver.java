package tools.vitruv.optggs.driver;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class ConstraintSolver {

    private static final String RETURN = "return";
    private static final String SELF = "self";
    private static final String DATA_TYPE_ESTRING = "EString";

    private static final String RESOURCE_PATH_TO_CONSTRAIN = "tools/vitruv/optggs/driver/constraints/%s.java";

    private final String name;
    private final String className;
    private final Path file;
    private final Collection<String> supportedBindings = new ArrayList<>();
    private final Collection<String> supportedGenBindings = new ArrayList<>();
    private final Map<String, String> parameters = new HashMap<>();

    public ConstraintSolver(String name, String className, Path file) {
        this.name = name;
        this.className = className;
        this.file = file;
    }

    public ConstraintSolver(String name, String className, URL url) {
        this(name, className, Path.of(url.getPath()));
    }

    public ConstraintSolver supportsBindings(Collection<String> bindings) {
        this.supportedBindings.addAll(bindings);
        return this;
    }

    public ConstraintSolver supportsGenBindings(Collection<String> genBindings) {
        this.supportedGenBindings.addAll(genBindings);
        return this;
    }

    public ConstraintSolver parameter(String name, String parameterType) {
        this.parameters.put(name, parameterType);
        return this;
    }

    public void copyFile(Path destinationDirectory) throws IOException {
        Files.copy(this.file, destinationDirectory.resolve(this.className + ".java"), StandardCopyOption.REPLACE_EXISTING);
    }

    public String name() {
        return name;
    }

    public String className() {
        return className;
    }

    public Collection<String> supportedBindings() {
        return supportedBindings;
    }

    public Collection<String> supportedGenBindings() {
        return supportedGenBindings;
    }

    public Collection<String> parameters() {
        // Order parameters to have a deterministic order
        // This is because eMoflon::neo gives us these parameters based on the index in the invocation, which
        // can be different than the index in the definition. And of course, we don't get the name of the parameter.
        // Alphabetical ordering, but `self` is the first and `result` the last entry.
        // E.g.: (a, c, self, return, b) becomes (self, a, b, c, return)
        return parameters.keySet().stream().sorted((a, b) -> switch (a) {
            case SELF -> -1;
            case RETURN -> 1;
            default -> switch (b) {
                case SELF -> -1;
                case RETURN -> 1;
                default -> a.compareTo(b);
            };
        }).toList();
    }

    public String parameterType(String parameter) {
        return this.parameters.get(parameter);
    }

    public static Collection<ConstraintSolver> defaultSolvers() {
        var solvers = new ArrayList<ConstraintSolver>();
        solvers.add(
            constraintSolverFromClass("concat", "Concat")
                .parameter(SELF, DATA_TYPE_ESTRING)
                .parameter("text", DATA_TYPE_ESTRING)
                .parameter(RETURN, DATA_TYPE_ESTRING)
                .supportsBindings(List.of("B B B", "B B F", "B F B", "F B B", "B F F", "F B F", "F F B", "F F F"))
                .supportsGenBindings(List.of("B B B", "F F F"))
        );
        solvers.add(
            constraintSolverFromClass("startsWith", "StartsWith")
                .parameter(SELF, DATA_TYPE_ESTRING)
                .parameter("prefix", DATA_TYPE_ESTRING)
                .supportsBindings(List.of("B B", "B F", "F B"))
                .supportsGenBindings(List.of("B B", "F F"))
        );
        return solvers;
    }

    private static ConstraintSolver constraintSolverFromClass(String name, String className) {
        var classLoader = ConstraintSolver.class.getClassLoader();
        return new ConstraintSolver(name, className, classLoader.getResource(RESOURCE_PATH_TO_CONSTRAIN.formatted(className)));
    }
}

