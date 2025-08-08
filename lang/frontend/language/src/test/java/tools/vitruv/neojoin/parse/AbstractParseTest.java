package tools.vitruv.neojoin.parse;

import org.eclipse.xtext.validation.Issue;
import tools.vitruv.neojoin.AbstractIntegrationTest;
import tools.vitruv.neojoin.ast.ViewTypeDefinition;
import tools.vitruv.neojoin.utils.Pair;

import java.util.List;

public class AbstractParseTest extends AbstractIntegrationTest {

    @Override
    protected List<String> getMetaModelPaths() {
        return List.of("/models/restaurant.ecore", "/models/reviewpage.ecore");
    }

    protected Pair<ViewTypeDefinition, List<Issue>> parse(String query) {
        return internalParse("""
            export package to "http://example.com"
            
            import "http://example.org/restaurant" as rest
            import "http://example.org/reviewpage"
            
            """ + query);
    }

}
