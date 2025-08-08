package tools.vitruv.neojoin.validation;


import com.google.inject.Inject;
import org.eclipse.xtext.validation.Check;
import org.eclipse.xtext.validation.ComposedChecks;
import tools.vitruv.neojoin.Constants;
import tools.vitruv.neojoin.ast.AstPackage;
import tools.vitruv.neojoin.ast.MainQuery;
import tools.vitruv.neojoin.ast.ViewTypeDefinition;
import tools.vitruv.neojoin.jvmmodel.ExpressionHelper;
import tools.vitruv.neojoin.utils.AstUtils;

import java.util.stream.Collectors;

/**
 * This class and the linked {@link ComposedChecks composed validators} contain custom validation rules.
 * See <a href="https://eclipse.dev/Xtext/documentation/303_runtime_concepts.html#validation">here</a>.
 */
@ComposedChecks(validators = {ImportValidator.class, SourceValidator.class, FeatureValidator.class, FeatureModifierValidator.class})
public class NeoJoinValidator extends AbstractNeoJoinValidator {

    @Inject
    private ExpressionHelper expressionHelper;

    @Check
    public void duplicatedTargetClassName(ViewTypeDefinition viewType) {
        var groupedTargets = AstUtils.getAllQueries(viewType)
            .collect(Collectors.groupingBy(q -> AstUtils.getTargetName(q, expressionHelper)));
        groupedTargets.entrySet().stream()
            .filter(entry -> !entry.getKey().equals(AstUtils.UnknownTargetName))
            .filter(entry -> entry.getValue().size() > 1)
            .forEach(entry -> {
                for (var query : entry.getValue()) {
                    error(
                        "Duplicated target class named '%s'".formatted(entry.getKey()),
                        query,
                        query.getName() != null ? AstPackage.Literals.QUERY__NAME : null
                    );
                }
            });

        var hasImplicitRoot = viewType.getQueries().stream().noneMatch(MainQuery::isRoot);
        if (hasImplicitRoot) {
            var conflicts = groupedTargets.get(Constants.DefaultRootClassName);
            if (conflicts != null) {
                conflicts.forEach(query -> {
                    error(
                        ("Target class name '%s' collides with name of the implicit root class. " +
                            "Either choose a different name for this class or explicitly create a root class with a different name.")
                            .formatted(query.getName()),
                        query,
                        query.getName() != null ? AstPackage.Literals.QUERY__NAME : null
                    );
                });
            }
        }
    }

    @Check
    public void checkJoinWithoutBody(MainQuery mainQuery) {
        var hasJoin = mainQuery.getSource() != null && !mainQuery.getSource().getJoins().isEmpty();
        if (hasJoin && mainQuery.getBody() == null) {
            error("Query with a join must have a body", mainQuery, AstPackage.Literals.QUERY__BODY);
        }
    }

    @Check
    public void checkSingleRoot(ViewTypeDefinition viewType) {
        var allRoots = viewType.getQueries().stream().filter(MainQuery::isRoot).toList();
        if (allRoots.size() > 1) {
            for (var query : allRoots) {
                error("Multiple root queries", query, AstPackage.Literals.MAIN_QUERY__ROOT);
            }
        }
    }

}
