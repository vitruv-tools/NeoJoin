package tools.vitruv.neojoin.validation;

import org.eclipse.xtext.validation.Check;
import tools.vitruv.neojoin.ast.AstPackage;
import tools.vitruv.neojoin.ast.From;
import tools.vitruv.neojoin.ast.JoinFeatureCondition;
import tools.vitruv.neojoin.ast.MainQuery;
import tools.vitruv.neojoin.ast.Source;
import tools.vitruv.neojoin.utils.AstUtils;

import java.util.stream.Collectors;

public class SourceValidator extends ComposableValidator {

    @Check
    public void checkUniqueAliases(MainQuery mainQuery) {
        if (mainQuery.getSource() == null) {
            return;
        }

        var groupedAliases = AstUtils.getAllFroms(mainQuery.getSource())
            .collect(Collectors.groupingBy(From::getAlias));

        groupedAliases.entrySet().stream()
            .filter(entry -> entry.getValue().size() > 1)
            .forEach(entry -> {
                for (var from : entry.getValue()) {
                    error("Duplicated alias: " + entry.getKey(), from, AstPackage.Literals.FROM__ALIAS);
                }
            });
    }

    @Check
    public void checkJoinConditionHasOtherInQueryWithMulti(JoinFeatureCondition condition) {
        if (condition.getOther() == null) {
            var source = (Source) condition.eContainer().eContainer();
            if (source.getJoins().size() > 1) {
                error(
                    "Join condition in a query with multiple joins must specify which other class to join on",
                    condition,
                    AstPackage.Literals.JOIN_FEATURE_CONDITION__OTHER
                );
            }
        }
    }

    @Check
    public void checkQueryWithoutSource(MainQuery mainQuery) {
        if (mainQuery.getSource() == null) {
            if (mainQuery.getName() == null) {
                error("Query without source must have a target name", mainQuery, AstPackage.Literals.QUERY__NAME);
            }
            if (mainQuery.getBody() == null) {
                error("Query without source must have a body", mainQuery, AstPackage.Literals.QUERY__BODY);
            }
        }
    }

    @Check
    public void checkQueryWithAggregation(MainQuery mainQuery) {
        if (mainQuery.getSource() == null || mainQuery.getSource().getGroupingExpressions().isEmpty()) {
            return;
        }

        if (mainQuery.getBody() == null) {
            error(
                "Query with aggregation must have a body",
                mainQuery.getSource(),
                AstPackage.Literals.SOURCE__GROUPING_EXPRESSIONS
            );
        }
    }

}
