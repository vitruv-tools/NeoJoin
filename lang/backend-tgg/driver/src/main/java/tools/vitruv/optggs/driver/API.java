package tools.vitruv.optggs.driver;

import org.eclipse.emf.common.util.URI;
import tools.vitruv.optggs.operators.Tuple;
import tools.vitruv.optggs.transpiler.*;
import tools.vitruv.optggs.operators.View;

import java.nio.file.Path;

public class API {

    public static void generateProjectForView(Project project, View view, Path location, NameResolver nameResolver) {
        System.out.println("Generating Project " + project.name() + " in " + location + " for View:");
        System.out.println(view);
        var scaffolding = new Scaffolding(location);
        scaffolding.create(project);
        var resolver = new TranspilerQueryResolver();
        var grammar = resolver.resolveView(view).toGrammar(project.name());
        TripleGraphPrinter.print(grammar, scaffolding.transformationFilePath(), nameResolver);
    }

}
