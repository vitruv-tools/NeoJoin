package tools.vitruv.neojoin.validation;


import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.xtext.validation.Check;
import org.eclipse.xtext.validation.ComposedChecks;
import tools.vitruv.neojoin.Constants;
import tools.vitruv.neojoin.aqr.AQR;
import tools.vitruv.neojoin.aqr.AQRBuilder;
import tools.vitruv.neojoin.ast.AstPackage;
import tools.vitruv.neojoin.ast.MainQuery;
import tools.vitruv.neojoin.ast.ViewTypeDefinition;
import tools.vitruv.neojoin.conformancevalidator.ConformanceValidator;
import tools.vitruv.neojoin.conformancevalidator.Violation;
import tools.vitruv.neojoin.generation.MetaModelGenerator;
import tools.vitruv.neojoin.jvmmodel.ExpressionHelper;
import tools.vitruv.neojoin.utils.AstUtils;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class and the linked {@link ComposedChecks composed validators} contain custom validation rules.
 * See <a href="https://eclipse.dev/Xtext/documentation/303_runtime_concepts.html#validation">here</a>.
 */
@ComposedChecks(validators = {ImportValidator.class, SourceValidator.class, FeatureValidator.class, FeatureModifierValidator.class})
public class NeoJoinValidator extends AbstractNeoJoinValidator {

    @Inject
    private ExpressionHelper expressionHelper;
    @Inject
    @Named(Constants.ImportPackageRegistry)
    private EPackage.Registry registry;

    @Check
    public void duplicatedParameterAlias(ViewTypeDefinition viewType) {
        var seen = new HashSet<String>();
        for (var param : viewType.getParameters()) {
            if (!seen.add(param.getAlias())) {
                error(
                    "Duplicate parameter name '%s'".formatted(param.getAlias()),
                    param,
                    AstPackage.Literals.PARAMETER__ALIAS
                );
            }
        }
    }

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
                conflicts.forEach(query ->
                    error(
                        ("Target class name '%s' collides with name of the implicit root class. " +
                            "Either choose a different name for this class or explicitly create a root class with a different name.")
                            .formatted(Constants.DefaultRootClassName),
                        query,
                        query.getName() != null ? AstPackage.Literals.QUERY__NAME : null
                    )
                );
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

    @Check
    public void checkConformance(ViewTypeDefinition viewType) {
        if (viewType.getConforms() == null) return;

        String uri = viewType.getConforms().getUri();
        EPackage referencePackage = registry.getEPackage(uri);
        if (referencePackage == null) {
            error("Reference metamodel not found: " + uri,
                AstPackage.Literals.VIEW_TYPE_DEFINITION__CONFORMS);
            return;
        }
        
        AQR aqr = new AQRBuilder(viewType, expressionHelper).build();
        EPackage viewTypePackage = new MetaModelGenerator(aqr).generate().pack();

        List<Violation> violations = ConformanceValidator.validatePackages(viewTypePackage, referencePackage);
        for (Violation v : violations) {
            error(v.toString(), AstPackage.Literals.VIEW_TYPE_DEFINITION__CONFORMS);
        }
    }

}
