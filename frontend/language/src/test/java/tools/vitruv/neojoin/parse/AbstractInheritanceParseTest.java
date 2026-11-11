package tools.vitruv.neojoin.parse;

import org.eclipse.xtext.validation.Issue;
import tools.vitruv.neojoin.ast.ViewTypeDefinition;
import tools.vitruv.neojoin.common.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for parse tests that need a source meta-model with an inheritance hierarchy ({@code kennel.ecore}).
 */
public abstract class AbstractInheritanceParseTest extends AbstractParseTest {

    @Override
    protected List<String> getMetaModelPaths() {
        var paths = new ArrayList<>(super.getMetaModelPaths());
        paths.add("/models/kennel.ecore");
        return paths;
    }

    /**
     * Like {@link #parse(String)} but additionally imports the kennel meta-model.
     */
    protected Pair<ViewTypeDefinition, List<Issue>> parseWithKennel(String query) {
        return internalParse("""
            export package to "http://example.com"

            import "http://example.org/restaurant" as rest
            import "http://example.org/reviewpage"
            import "http://example.org/kennel"

            """ + query);
    }

}
