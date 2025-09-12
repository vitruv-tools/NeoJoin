package tools.vitruv.neojoin.jvmmodel;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.common.types.JvmGenericType;
import org.eclipse.xtext.common.types.JvmOperation;
import org.eclipse.xtext.common.types.JvmType;
import org.eclipse.xtext.common.types.JvmTypeReference;
import org.eclipse.xtext.common.types.JvmVisibility;
import org.eclipse.xtext.common.types.TypesFactory;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.compiler.DisableCodeGenerationAdapter;
import org.eclipse.xtext.xbase.jvmmodel.IJvmDeclaredTypeAcceptor;
import org.eclipse.xtext.xbase.jvmmodel.JvmTypeReferenceBuilder;
import org.eclipse.xtext.xbase.jvmmodel.JvmTypesBuilder;
import org.jspecify.annotations.Nullable;
import tools.vitruv.neojoin.Constants;
import tools.vitruv.neojoin.ast.Body;
import tools.vitruv.neojoin.ast.From;
import tools.vitruv.neojoin.ast.MainQuery;
import tools.vitruv.neojoin.ast.Source;
import tools.vitruv.neojoin.ast.ViewTypeDefinition;
import tools.vitruv.neojoin.utils.AstUtils;
import tools.vitruv.neojoin.utils.Utils;

import java.util.List;
import java.util.function.Consumer;

import static tools.vitruv.neojoin.utils.Assertions.check;

/**
 * Embeds all expression of the given query into methods to define available local variables and the expected return type.
 */
public class QueryModelInferrer {

    private final IJvmDeclaredTypeAcceptor acceptor;
    private final ViewTypeDefinition viewType;
    private final TypeRegistry sourceTypes;
    private final JvmTypesBuilder types;
    private final JvmTypeReferenceBuilder typeReferences;
    private final ExpressionHelper expressionHelper;

    private @Nullable JvmGenericType root;

    public QueryModelInferrer(
        IJvmDeclaredTypeAcceptor acceptor,
        ViewTypeDefinition viewType,
        TypeRegistry sourceTypes,
        JvmTypesBuilder types,
        JvmTypeReferenceBuilder typeReferences,
        ExpressionHelper expressionHelper
    ) {
        this.acceptor = acceptor;
        this.viewType = viewType;
        this.sourceTypes = sourceTypes;
        this.types = types;
        this.typeReferences = typeReferences;
        this.expressionHelper = expressionHelper;
    }

    public void infer() {
        check(root == null, "already inferred");

        root = TypesFactory.eINSTANCE.createJvmGenericType();
        root.setSimpleName("AllQueryExpressions");
        root.setVisibility(JvmVisibility.PRIVATE);
        DisableCodeGenerationAdapter.disableCodeGeneration(root); // otherwise Xtext generates java files in our working directory

        // prevent name collisions with other open query documents by choosing a unique name for the package
        if (viewType.getExport() != null) {
            root.setPackageName("%s@[%s]".formatted(viewType.getExport().getPackage(), viewType.getExport().getUri()));
        } else {
            root.setPackageName("invalid$%d".formatted(System.identityHashCode(viewType)));
        }

        viewType.eResource().getContents().add(root); // otherwise type resolution fails
        for (MainQuery q : viewType.getQueries()) {
            inferMainQuery(q);
        }
        viewType.eResource().getContents().remove(root);

        acceptor.accept(root);
    }

    private void inferMainQuery(MainQuery mainQuery) {
        var targetName = AstUtils.getTargetName(mainQuery);

        if (mainQuery.getSource() != null) {
            Utils.forEachIndexed(
                mainQuery.getSource().getJoins(), (join, joinIndex) -> {
                    Utils.forEachIndexed(
                        join.getExpressionConditions(), (condition, conditionIndex) -> {
                            addExpression(
                                join,
                                "%s_join_%d_condition_%d".formatted(targetName, joinIndex, conditionIndex),
                                "boolean",
                                condition.getExpression(),
                                paramsForSource(mainQuery.getSource(), false, join.getFrom())
                            );
                        }
                    );
                }
            );

            if (mainQuery.getSource().getCondition() != null) {
                addExpression(
                    mainQuery.getSource(),
                    targetName + "_condition",
                    "boolean",
                    mainQuery.getSource().getCondition(),
                    paramsForSource(mainQuery.getSource(), false, null)
                );
            }

            Utils.forEachIndexed(
                mainQuery.getSource().getGroupingExpressions(), (condition, index) -> {
                    addExpression(
                        mainQuery.getSource(),
                        targetName + "_grouping_" + index,
                        "java.lang.Object", // grouping expression can be anything
                        condition,
                        paramsForSource(mainQuery.getSource(), false, null)
                    );
                }
            );
        }

        if (mainQuery.getBody() != null) {
            inferBody(
                mainQuery.getBody(),
                targetName,
                paramsForSource(mainQuery.getSource(), AstUtils.isGrouping(mainQuery.getSource()), null)
            );
        }
    }

    private void inferBody(Body body, String name, Consumer<JvmOperation> addParams) {
        Utils.forEachIndexed(
            body.getFeatures(), (feature, index) -> {
                var exprName = name + "_feature_" + index;
                // expression type is validated using a custom validator
                addExpression(feature, exprName, "java.lang.Object", feature.getExpression(), addParams);

                if (feature.getSubQuery() != null && feature.getSubQuery().getBody() != null) {
                    var featureType = inferEClassFromExpressionOrNull(feature.getExpression());
                    if (featureType != null) {
                        var subQueryName = AstUtils.getTargetName(feature.getSubQuery(), featureType);
                        inferBody(
                            feature.getSubQuery().getBody(),
                            subQueryName,
                            paramsForClass(featureType, feature.getSubQuery())
                        );
                    }
                }
            }
        );
    }

    private @Nullable EClass inferEClassFromExpressionOrNull(XExpression expression) {
        // Type resolution in Xbase is caching by default. This is problematic because the generated query model is
        // potentially still incomplete at this point if further expressions follow. If we were to trigger type resolution
        // with caching here, the types of all following expressions would never be resolved because there would already
        // exist a cache entry for the current resource.
        var typeInfo = expressionHelper.<@Nullable TypeInfo>execUncached(
            expression.eResource(), () -> {
                try {
                    return expressionHelper.inferEType(expression);
                } catch (TypeResolutionException e) {
                    // ignore: will be handled by type checking
                    return null;
                }
            }
        );
        if (typeInfo != null && typeInfo.classifier() instanceof EClass clazz) {
            return clazz;
        } else {
            return null;
        }
    }

    /**
     * Expose an expression to Xtext by defining a method with the available local variables as parameters and the expected
     * expression type as return type of the method.
     *
     * @param source     the {@link JvmTypesBuilder} requires a source element to associate with the newly generated method
     * @param name       name of the expression
     * @param type       expected type of the expression
     * @param expression expression
     * @param addParams  function to define local variables (= method parameters)
     */
    private void addExpression(
        EObject source,
        String name,
        String type,
        XExpression expression,
        Consumer<JvmOperation> addParams
    ) {
        check(root != null);
        root.getMembers().add(types.toMethod(
            source, name, typeReferences.typeRef(type), (op) -> {
                op.setStatic(true);
                addParams.accept(op);
                types.setBody(op, expression);
            }
        ));
    }

    /**
     * Returns a lambda that creates parameters for each {@link From} within the given source.
     *
     * @param source     source element
     * @param isGrouping whether the context is grouping
     * @param limit      limit
     * @return lambda that creates parameters
     */
    private Consumer<JvmOperation> paramsForSource(@Nullable Source source, boolean isGrouping, @Nullable From limit) {
        if (source == null) {
            return op -> {};
        } else {
            return op -> {
                if (AstUtils.getAllFroms(source).count() <= 1) {
                    addParam(
                        op,
                        source.getFrom(),
                        Constants.ExpressionSelfReference,
                        sourceTypes.getClass(source.getFrom().getClazz()),
                        isGrouping
                    );
                }

                Iterable<From> allFroms = () -> AstUtils.getAllFroms(source).iterator();
                for (var from : allFroms) {
                    if (from.getAlias() != null) {
                        addParam(op, from, from.getAlias(), sourceTypes.getClass(from.getClazz()), isGrouping);
                    }

                    if (from == limit) {
                        break;
                    }
                }
            };
        }
    }


    /**
     * Returns a lambda that creates a single parameter named {@link Constants#ExpressionSelfReference} for the given class.
     *
     * @param source source element
     * @return lambda that creates the parameter
     */
    private Consumer<JvmOperation> paramsForClass(EClass clazz, EObject source) {
        return op -> {
            addParam(op, source, Constants.ExpressionSelfReference, sourceTypes.getClass(clazz), false);
        };
    }

    /**
     * Adds a parameter to the given {@link JvmOperation} (= method).
     *
     * @param op         jvm operation
     * @param source     the {@link JvmTypesBuilder} requires a source element to associate with the newly generated parameter
     * @param name       name of the parameter
     * @param type       type of the parameter
     * @param isGrouping whether the parameter is grouping -> list type
     * @implNote I'm not sure how the given source element is used internally within Xtext. But it seems to allow IDE actions like
     * go-to-definition.
     */
    private void addParam(JvmOperation op, EObject source, String name, @Nullable JvmType type, boolean isGrouping) {
        var typeRef = isGrouping ? wrapInList(typeRef(type)) : typeRef(type);
        op.getParameters().add(types.toParameter(source, name, typeRef));
    }

    private JvmTypeReference typeRef(@Nullable JvmType type) {
        if (type != null) {
            return typeReferences.typeRef(type);
        } else {
            return typeReferences.typeRef("invalid");
        }
    }

    private JvmTypeReference wrapInList(JvmTypeReference typeRef) {
        return typeReferences.typeRef(List.class, typeRef);
    }

}
