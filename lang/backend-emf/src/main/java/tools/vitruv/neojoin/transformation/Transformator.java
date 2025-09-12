package tools.vitruv.neojoin.transformation;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;
import org.jspecify.annotations.Nullable;
import tools.vitruv.neojoin.Constants;
import tools.vitruv.neojoin.aqr.AQR;
import tools.vitruv.neojoin.aqr.AQRFeature;
import tools.vitruv.neojoin.aqr.AQRTargetClass;
import tools.vitruv.neojoin.jvmmodel.ExpressionHelper;
import tools.vitruv.neojoin.transformation.source.GroupingSource;
import tools.vitruv.neojoin.transformation.source.InstanceSourceFactory;
import tools.vitruv.neojoin.utils.TypeCasts;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static tools.vitruv.neojoin.utils.Assertions.check;
import static tools.vitruv.neojoin.utils.Assertions.fail;

/**
 * Transforms the given source instance models based on the given {@link AQR query}.
 *
 * @see #transform()
 */
public class Transformator {

    private final ExpressionHelper expressionHelper;
    private final AQR aqr;
    private final EPackage targetMetaModel;
    private final InstanceSourceFactory instanceSourceFactory;

    private @Nullable EObject root;
    private final TargetMap targetMap = new TargetMap();
    private final List<Runnable> delayedActions = new ArrayList<>();

    /**
     * Creates a new transformator for transforming the given source instance models into an instance of the given
     * target meta-model based on the given query.
     *
     * @param expressionHelper     for evaluating feature expressions and conditions
     * @param aqr                  query representation
     * @param targetMetaModel      target meta-model that corresponds to the given query representation
     * @param sourceInstanceModels map from {@link EPackage package} to the corresponding instance model {@link Resource resource}
     */
    public Transformator(
        ExpressionHelper expressionHelper,
        AQR aqr,
        EPackage targetMetaModel,
        Map<EPackage, Resource> sourceInstanceModels
    ) {
        this.expressionHelper = expressionHelper;
        this.aqr = aqr;
        this.targetMetaModel = targetMetaModel;
        this.instanceSourceFactory = new InstanceSourceFactory(sourceInstanceModels);
    }

    /**
     * Register an action to be executed after the instance creation phase.
     */
    private void later(Runnable action) {
        delayedActions.add(action);
    }

    /**
     * Transforms the given source instance models.
     *
     * @throws TransformatorException if a user caused error arises during transformation
     *                                (e.g. exception while evaluating an expression)
     * @implNote Transformation is performed in 2 phases: First all instances in the target model are created but its features
     * not yet populated because an instance could reference another instance that has not been created yet. In the
     * second phase, the features for all instances are populated.
     */
    public EObject transform() throws TransformatorException {
        check(root == null, "model already transformed");

        // phase 1:
        // create root instance
        var roots = transformTargetClass(aqr.root());
        check(
            roots.size() == 1, () -> "expected exactly one root instance of type %s, but found %d".formatted(
                aqr.root().name(),
                roots.size()
            )
        );
        root = roots.getFirst();

        // create other instances
        aqr.classes().stream()
            .filter(target -> target != aqr.root())
            .forEach(target -> {
                var instances = transformTargetClass(target);
                var rootRef = root.eClass()
                    .getEStructuralFeature(Constants.RootReferenceNameFormat.formatted(target.name()));
                root.eSet(rootRef, instances);
            });

        // phase 2: populate instances
        delayedActions.forEach(Runnable::run);
        delayedActions.clear();

        return root;
    }

    private EClass getTargetClass(String name) {
        var clazz = targetMetaModel.getEClassifier(name);
        check(clazz != null, () -> "no class named '%s' found in target meta-model".formatted(name));
        return (EClass) clazz;
    }

    private List<EObject> transformTargetClass(AQRTargetClass targetClass) {
        var clazz = getTargetClass(targetClass.name());

        if (targetClass.source() == null) { // no source -> create a single instance
            return List.of(createTransformedInstance(targetClass, clazz));
        } else {
            var evaluator = new ExpressionEvaluator(expressionHelper, targetClass.source());
            var instanceSource = instanceSourceFactory.create(targetClass.source(), evaluator);

            if (targetClass.source().groupingExpressions().isEmpty()) { // no grouping
                return instanceSource.get()
                    .map(tuple -> createTransformedInstance(targetClass, clazz, tuple, evaluator))
                    .toList();
            } else { // with grouping
                var groupingSource = new GroupingSource(
                    targetClass.source().groupingExpressions(),
                    instanceSource,
                    evaluator
                );
                return groupingSource.get()
                    .map(tupleOfLists -> createTransformedInstance(targetClass, clazz, tupleOfLists, evaluator))
                    .toList();
            }
        }
    }

    /**
     * Create a transformed instance of the given target class.
     *
     * @param targetClass AQR target class
     * @param clazz       EClass of the target class
     * @param mainSource  main source instance to retrieve values from or {@code null} if no main source is available
     * @param allSources  all source instances to register with the created target instance
     * @param context     expression evaluation context
     * @return transformed instance
     */
    private EObject createTransformedInstance(
        AQRTargetClass targetClass,
        EClass clazz,
        @Nullable EObject mainSource,
        Stream<@Nullable EObject> allSources,
        ExpressionEvaluator.Context context
    ) {
        check(clazz.getEPackage() == targetMetaModel);
        var targetInstance = targetMetaModel.getEFactoryInstance().create(clazz);
        registerTargetInstance(targetInstance, targetClass, allSources);
        later(() -> populateTargetInstance(targetClass, targetInstance, mainSource, context));
        return targetInstance;
    }

    /**
     * Create a transformed instance of the given target class without source.
     *
     * @param targetClass AQR target class
     * @param clazz       EClass of the target class
     * @return transformed instance
     */
    private EObject createTransformedInstance(AQRTargetClass targetClass, EClass clazz) {
        check(targetClass.source() == null);
        var context = ExpressionEvaluator.createContext(expressionHelper);
        return createTransformedInstance(targetClass, clazz, null, Stream.of(), context);
    }

    /**
     * Create a transformed instance of the given target class.
     *
     * @param targetClass   AQR target class
     * @param clazz         EClass of the target class
     * @param instanceTuple instance tuple
     * @param evaluator     expression evaluator
     * @return transformed instance
     */
    private EObject createTransformedInstance(
        AQRTargetClass targetClass,
        EClass clazz,
        InstanceTuple instanceTuple,
        ExpressionEvaluator evaluator
    ) {
        var mainSource = instanceTuple.stream().findFirst().orElseThrow();
        var context = evaluator.createContext(instanceTuple, null);
        return createTransformedInstance(targetClass, clazz, mainSource, instanceTuple.stream(), context);
    }

    /**
     * Transform target class with the given grouping instance source.
     *
     * @param targetClass  AQR target class
     * @param clazz        EClass of the target class
     * @param tupleOfLists tuple of instance lists
     * @param evaluator    expression evaluator
     * @return list of transformed instances
     */
    private EObject createTransformedInstance(
        AQRTargetClass targetClass,
        EClass clazz,
        List<List<EObject>> tupleOfLists,
        ExpressionEvaluator evaluator
    ) {
        var context = evaluator.createContext(tupleOfLists.iterator(), null);
        return createTransformedInstance(
            targetClass,
            clazz,
            null,
            tupleOfLists.stream().flatMap(List::stream),
            context
        );
    }

    private void registerTargetInstance(
        EObject targetInstance,
        AQRTargetClass targetClass,
        Stream<@Nullable EObject> sources
    ) {
        sources.filter(Objects::nonNull).forEach(source -> {
            targetMap.register(source, targetClass, targetInstance);
        });
    }

    private void populateTargetInstance(
        AQRTargetClass targetClass,
        EObject target,
        @Nullable EObject source,
        ExpressionEvaluator.Context context
    ) {
        for (var feature : targetClass.features()) {
            if (feature.kind() instanceof AQRFeature.Kind.Generate) {
                continue; // generated features are populated elsewhere
            }

            if (feature instanceof AQRFeature.Attribute attr) {
                populateAttribute(attr, target, source, context);
            } else if (feature instanceof AQRFeature.Reference ref) {
                populateReference(ref, target, source, context);
            }
        }
    }

    private void populateAttribute(
        AQRFeature.Attribute attribute,
        EObject target,
        @Nullable EObject source,
        ExpressionEvaluator.Context context
    ) {
        var feature = target.eClass().getEStructuralFeature(attribute.name());
        var value = evaluateFeature(attribute.kind(), source, context);
        if (value instanceof EEnumLiteral enumLiteral) {
            var targetEnum = (EEnum) feature.getEType();
            value = targetEnum.getEEnumLiteral(enumLiteral.getName());
            check(value != null);
        }
        value = cast(value, feature.getEType(), feature.isMany());
        target.eSet(feature, value);
    }

    private @Nullable Object cast(@Nullable Object value, EClassifier to, boolean isMany) {
        if (value == null) {
            return null;
        }

        if (to.getInstanceClass() == null) {
            return value;
        }

        if (isMany) {
            var list = (List<?>) value;
            return list.stream()
                .map(v -> cast(v, to, false))
                .toList();
        } else {
            return TypeCasts.cast(value, to.getInstanceClass());
        }
    }

    private void populateReference(
        AQRFeature.Reference ref,
        EObject target,
        @Nullable EObject source,
        ExpressionEvaluator.Context context
    ) {
        var value = evaluateFeature(ref.kind(), source, context);
        var mappedValue = value != null ? mapInstances(value, ref.type()) : null;
        if (mappedValue != null && ref.options().isContainment()) {
            checkNotAlreadyContained(mappedValue, target, ref);
        }
        target.eSet(target.eClass().getEStructuralFeature(ref.name()), mappedValue);
    }

    private Object mapInstances(Object instance, AQRTargetClass target) {
        if (instance instanceof List<?> list) {
            return list.stream().map(i -> targetMap.get((EObject) i, target)).toList();
        } else {
            return targetMap.get((EObject) instance, target);
        }
    }

    private void checkNotAlreadyContained(Object value, EObject target, AQRFeature.Reference ref) {
        if (value instanceof List<?> list) {
            list.forEach(v -> checkNotAlreadyContained((EObject) v, target, ref));
        } else {
            checkNotAlreadyContained((EObject) value, target, ref);
        }
    }

    private void checkNotAlreadyContained(EObject value, EObject target, AQRFeature.Reference ref) {
        if (value.eContainer() != null && value.eContainer() != root) {
            throw new TransformatorException(
                "cannot add target instance of class '%s' to containment reference '%s.%s' because it is already contained in another instance of class '%s'".formatted(
                    value.eClass().getName(),
                    target.eClass().getName(),
                    ref.name(),
                    value.eContainer().eClass().getName()
                ));
        }
    }

    private @Nullable Object evaluateFeature(
        AQRFeature.Kind featureKind,
        @Nullable EObject source,
        ExpressionEvaluator.Context context
    ) {
        if (featureKind.expression() != null) {
            return context.evaluateExpression(Objects.requireNonNull(featureKind.expression()));
        } else if (featureKind instanceof AQRFeature.Kind.Copy.Implicit(EStructuralFeature feature)) {
            check(source != null && source.eClass() == feature.getEContainingClass());
            return source.eGet(feature);
        } else {
            return fail();
        }
    }

}
