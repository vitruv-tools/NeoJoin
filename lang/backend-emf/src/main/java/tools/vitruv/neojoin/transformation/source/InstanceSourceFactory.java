package tools.vitruv.neojoin.transformation.source;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.xbase.XExpression;
import tools.vitruv.neojoin.aqr.AQRFrom;
import tools.vitruv.neojoin.aqr.AQRJoin;
import tools.vitruv.neojoin.aqr.AQRSource;
import tools.vitruv.neojoin.transformation.ExpressionEvaluator;
import tools.vitruv.neojoin.utils.EMFUtils;

import java.util.Map;

import static tools.vitruv.neojoin.utils.Assertions.require;

/**
 * Creates an {@link InstanceSource} for the given {@link AQRSource}.
 * <p>
 * <b>Note:</b> Does <b>not</b> handle {@link GroupingSource}.
 */
public class InstanceSourceFactory {

    private final Map<EPackage, Resource> sourceInstanceModels;

    public InstanceSourceFactory(Map<EPackage, Resource> sourceInstanceModels) {
        this.sourceInstanceModels = sourceInstanceModels;
    }

    public InstanceSource create(AQRSource source, ExpressionEvaluator evaluator) {
        InstanceSource result = createFrom(source.from());
        for (var join : source.joins()) {
            result = createJoin(join, result, createFrom(join.from()), evaluator);
        }
        if (source.condition() != null) {
            result = createFilter(source.condition(), result, evaluator);
        }
        return result;
    }

    public FromSource createFrom(AQRFrom from) {
        var rootPackage = EMFUtils.getRootPackage(from.clazz().getEPackage());
        var referencedInstanceModel = sourceInstanceModels.get(rootPackage);
        require(
            referencedInstanceModel != null,
            () -> "referenced instance model not found for package '%s'".formatted(from.clazz().getEPackage().getName())
        );

        return new FromSource(from.clazz(), referencedInstanceModel);
    }

    public InstanceSource createJoin(
        AQRJoin join,
        InstanceSource left,
        FromSource right,
        ExpressionEvaluator evaluator
    ) {
        return switch (join.type()) {
            case Inner -> new InnerJoinSource(left, right, join, evaluator);
            case Left -> new LeftJoinSource(left, right, join, evaluator);
        };
    }

    private InstanceSource createFilter(XExpression condition, InstanceSource source, ExpressionEvaluator evaluator) {
        return new FilterSource(condition, source, evaluator);
    }

}
