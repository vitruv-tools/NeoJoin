package tools.vitruv.neojoin.aqr;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EStructuralFeature;
import tools.vitruv.neojoin.ast.From;
import tools.vitruv.neojoin.ast.Join;
import tools.vitruv.neojoin.ast.JoinExpressionCondition;
import tools.vitruv.neojoin.ast.JoinFeatureCondition;
import tools.vitruv.neojoin.ast.JoinType;
import tools.vitruv.neojoin.ast.Source;
import tools.vitruv.neojoin.utils.AstUtils;
import tools.vitruv.neojoin.utils.Utils;

import java.util.List;
import java.util.Objects;

import static tools.vitruv.neojoin.aqr.AQRInvariantViolatedException.invariant;

/**
 * Builds the {@link AQRSource source} of a query.
 *
 * @see AQRBuilder
 */
public class AQRSourceBuilder {

    /**
     * Creates a source for the given AST source.
     */
    public static AQRSource createSource(Source source) {
        return new AQRSource(
            createFrom(source.getFrom()),
            source.getJoins().stream().map(AQRSourceBuilder::createJoin).toList(),
            source.getCondition() != null ? source.getCondition() : null,
            source.getGroupingExpressions()
        );
    }

    /**
     * Creates a source for the given class.
     */
    public static AQRSource createSource(EClass source) {
        return new AQRSource(createFrom(source), List.of(), null, List.of());
    }

    /**
     * Creates an AQR join for the given AST join.
     */
    private static AQRJoin createJoin(Join join) {
        return new AQRJoin(
            mapJoinType(join.getType()),
            createFrom(join.getFrom()),
            join.getFeatureConditions().stream().map(AQRSourceBuilder::createJoinFeatureCondition).toList(),
            join.getExpressionConditions().stream().map(JoinExpressionCondition::getExpression).toList()
        );
    }

    /**
     * Map join type from AST to AQR.
     */
    private static AQRJoin.Type mapJoinType(JoinType type) {
        return switch (type) {
            case INNER -> AQRJoin.Type.Inner;
            case LEFT -> AQRJoin.Type.Left;
        };
    }

    /**
     * Creates an AQR from for the given AST from.
     */
    private static AQRFrom createFrom(From from) {
        return new AQRFrom(Objects.requireNonNull(from.getClazz()), from.getAlias());
    }

    /**
     * Creates an AQR from for the given source class.
     */
    private static AQRFrom createFrom(EClass from) {
        return new AQRFrom(from, null);
    }

    /**
     * Creates an AQR feature condition for the given AST feature condition.
     */
    private static AQRJoin.FeatureCondition createJoinFeatureCondition(JoinFeatureCondition condition) {
        var features = condition.getFeatures().stream().map(EStructuralFeature::getName).toList();
        var index = -1;
        if (condition.getOther() == null) {
            index = 0;
        } else {
            var source = (Source) condition.eContainer().eContainer();
            index = Utils.indexOf(AstUtils.getAllFroms(source), condition.getOther());
            invariant(index != -1);
        }
        return new AQRJoin.FeatureCondition(index, features);
    }

}
