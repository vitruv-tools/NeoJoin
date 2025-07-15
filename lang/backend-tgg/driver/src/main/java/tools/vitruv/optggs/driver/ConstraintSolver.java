package tools.vitruv.optggs.driver;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class ConstraintSolver {
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
        return parameters.keySet().stream().sorted((a, b) -> {
            if (a.equals("self")) {
                return -1;
            } else if (a.equals("return")) {
                return 1;
            } else if (b.equals("self")) {
                return 1;
            } else if (b.equals("return")) {
                return -1;
            } else {
                return a.compareTo(b);
            }
        }).toList();
    }

    public String parameterType(String parameter) {
        return this.parameters.get(parameter);
    }

    public static Collection<ConstraintSolver> defaultSolvers() {
        var classLoader = ConstraintSolver.class.getClassLoader();
        var solvers = new ArrayList<ConstraintSolver>();
        solvers.add(
                new ConstraintSolver("concat", "Concat", classLoader.getResource("tools/vitruv/optggs/driver/constraints/Concat.java"))
                        .parameter("self", "EString")
                        .parameter("text", "EString")
                        .parameter("return", "EString")
                        .supportsBindings(List.of("B B B", "B B F", "B F B", "F B B", "B F F", "F B F", "F F B", "F F F"))
                        .supportsGenBindings(List.of("B B B", "F F F"))
        );
        solvers.add(
                new ConstraintSolver("startsWith", "StartsWith", classLoader.getResource("tools/vitruv/optggs/driver/constraints/StartsWith.java"))
                        .parameter("self", "EString")
                        .parameter("prefix", "EString")
                        .supportsBindings(List.of("B B", "B F", "F B"))
                        .supportsGenBindings(List.of("B B", "F F"))
        );
        return solvers;
    }
}

