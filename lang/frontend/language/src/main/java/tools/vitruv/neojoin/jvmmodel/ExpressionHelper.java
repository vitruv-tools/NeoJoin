package tools.vitruv.neojoin.jvmmodel;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.impl.DynamicEObjectImpl;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.common.types.JvmField;
import org.eclipse.xtext.common.types.JvmType;
import org.eclipse.xtext.common.types.JvmVoid;
import org.eclipse.xtext.common.types.util.TypeReferences;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.util.OnChangeEvictingCache;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.XMemberFeatureCall;
import org.eclipse.xtext.xbase.interpreter.IEvaluationContext;
import org.eclipse.xtext.xbase.interpreter.IEvaluationResult;
import org.eclipse.xtext.xbase.interpreter.impl.XbaseInterpreter;
import org.eclipse.xtext.xbase.typesystem.IBatchTypeResolver;
import org.eclipse.xtext.xbase.typesystem.references.LightweightTypeReference;
import org.eclipse.xtext.xbase.typesystem.references.UnknownTypeReference;
import org.jspecify.annotations.Nullable;
import tools.vitruv.neojoin.Constants;
import tools.vitruv.neojoin.aqr.AQRFrom;
import tools.vitruv.neojoin.utils.EMFUtils;
import tools.vitruv.neojoin.utils.Result;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static tools.vitruv.neojoin.utils.Assertions.check;

/**
 * Utilities for handling {@link XExpression Xbase expressions}.
 *
 * @see #inferEType(XExpression)
 * @see #getFeatureOrNull(XExpression)
 * @see #evaluate(XExpression, IEvaluationContext)
 */
@Singleton
public class ExpressionHelper {

    /**
     * Custom interpreter that can handle field accesses on {@link DynamicEObjectImpl dynamic ecore objects}.
     */
    private static class Interpreter extends XbaseInterpreter {

        @Override
        protected Object featureCallField(JvmField jvmField, Object receiver) {
            if (receiver instanceof DynamicEObjectImpl eObject) {
                var field = eObject.eClass().getEStructuralFeature(jvmField.getSimpleName());
                check(field != null, () -> "Could not find field " + jvmField.getIdentifier());
                var value = eObject.eGet(field);
                if (value instanceof List<?> list) {
                    return Collections.unmodifiableList(list);
                } else {
                    return value;
                }
            }
            return super.featureCallField(jvmField, receiver);
        }

    }

    @Inject
    private Provider<IEvaluationContext> contextProvider;

    /**
     * Creates an empty evaluation context with no accessible variables.
     *
     * @return evaluation context to be used in {@link #evaluate(XExpression, IEvaluationContext) evaluate(..)}
     */
    public IEvaluationContext createContext() {
        return contextProvider.get();
    }

    /**
     * Creates an evaluation context taking variable names from the given {@link AQRFrom} iterator and
     * values from the given value iterator. Optionally takes a limit that makes variables encountered after the limit inaccessible.
     *
     * @param fromIterator  iterator of {@link AQRFrom} for variable names
     * @param valueIterator iterator of {@link Object} for variable values
     * @param limit         optional limit
     * @return evaluation context to be used in {@link #evaluate(XExpression, IEvaluationContext) evaluate(..)}
     * @see #createContext() if no variables are required
     */
    public IEvaluationContext createContext(
        Iterator<AQRFrom> fromIterator,
        Iterator<?> valueIterator,
        @Nullable AQRFrom limit
    ) {
        IEvaluationContext context = createContext();

        boolean isFirst = true;
        while (fromIterator.hasNext() && valueIterator.hasNext()) {
            var from = fromIterator.next();
            var value = valueIterator.next();

            if (isFirst) {
                isFirst = false;
                if (!fromIterator.hasNext()) {
                    context.newValue(QualifiedName.create(Constants.ExpressionSelfReference), value);
                }
            }

            if (from.alias() != null) {
                context.newValue(QualifiedName.create(from.alias()), value);
            }

            if (from == limit) {
                return context;
            }
        }

        check(limit == null);
        check(!fromIterator.hasNext() && !valueIterator.hasNext());

        return context;
    }

    @Inject
    private Interpreter interpreter;

    /**
     * Evaluate the given expression with the given context.
     *
     * @param expression expression to evaluate
     * @param context    context, i.e. accessible local variables
     * @return {@link Result.Success} with the returned value if execution was successful,
     * or {@link Result.Failure} with the thrown exception otherwise
     * @see #createContext(Iterator, Iterator, AQRFrom) creating the context
     */
    public Result<@Nullable Object> evaluate(XExpression expression, IEvaluationContext context) {
        IEvaluationResult result = interpreter.evaluate(expression, context, CancelIndicator.NullImpl);
        if (result.getException() != null) {
            return new Result.Failure<>(result.getException());
        } else {
            return new Result.Success<>(result.getResult());
        }
    }

    @Inject
    private IBatchTypeResolver typeResolver;

    /**
     * Resolves the type of the given {@link XExpression}.
     *
     * @param expression expression for type resolution
     * @return resolved type
     * @throws TypeResolutionException if type resolution failed
     * @implNote The {@link #typeResolver used type resolver} is caching which means that types are only computed once.
     * Type resolution works only if the given expression is part of a resource which is normally the case after
     * jvm model inference is complete (see {@link QueryModelInferrer}).
     */
    private LightweightTypeReference resolve(XExpression expression) throws TypeResolutionException {
        var resolved = typeResolver.resolveTypes(expression).getActualType(expression);
        if (resolved == null || resolved instanceof UnknownTypeReference) {
            throw new TypeResolutionException(expression);
        }
        return resolved;
    }

    /**
     * If the expression is a field access on an ecore object from a source model, returns the corresponding structural feature, otherwise returns {@code null}.
     *
     * @param expression the expression to check
     * @return the structural feature or {@code null}
     * @throws TypeResolutionException if the receiver type cannot be resolved
     */
    public @Nullable EStructuralFeature getFeatureOrNull(XExpression expression) throws TypeResolutionException {
        if (expression instanceof XMemberFeatureCall featureCall) {
            if (featureCall.getFeature() instanceof JvmVoid) {
                throw new TypeResolutionException(expression);
            }

            if (featureCall.getFeature() instanceof JvmField field) {
                check(featureCall.getActualReceiver() != null);
                var receiverType = resolve(featureCall.getActualReceiver());
                if (receiverType.getType() != null) {
                    var receiverClassifier = SourceModelInferrer.getEClassifierOrNull(receiverType.getType());
                    if (receiverClassifier instanceof EClass receiverClass) { // includes null check
                        // might be null for an invalid feature access but that's handled by Xbase type checking
                        return receiverClass.getEStructuralFeature(field.getSimpleName());
                    }
                }
            }
        }

        return null;
    }

    /**
     * Infers the type of the given expression and returns the corresponding Ecore {@link EDataType} or
     * {@link EClass} from one of the source models.
     *
     * @param expression expression to resolve types
     * @return {@link TypeInfo}, or {@code null} if Xbase type resolution succeeds but the returned java type
     * cannot be mapped to an {@link EClassifier}, e.g. when the type is unsupported like
     * {@link AtomicInteger}
     * @throws TypeResolutionException if Xbase type resolution fails
     */
    public @Nullable TypeInfo inferEType(XExpression expression) throws TypeResolutionException {
        var resolvedType = resolve(expression);
        var type = resolvedType.getType();
        if (type == null) {
            return new TypeInfo(null, false);
        }

        // unwrap list
        var isMany = false;
        var listType = tryGetListType(resolvedType);
        if (listType != null) {
            type = listType;
            isMany = true;
        }

        var classifier = SourceModelInferrer.getEClassifierOrNull(type);
        if (classifier != null) {
            return new TypeInfo(classifier, isMany);
        }

        var dataType = getEcoreDataType(type);
        if (dataType != null) {
            return new TypeInfo(dataType, isMany);
        }

        return null;
    }

    private static @Nullable Map<String, EDataType> ecoreTypeCache = null;

    private static @Nullable EDataType getEcoreDataType(JvmType jvmType) {
        if (ecoreTypeCache == null) {
            ecoreTypeCache = EMFUtils.getAllEDataTypes(EcorePackage.eINSTANCE)
                .collect(Collectors.toMap(d -> Objects.requireNonNull(d.getInstanceClassName()), dataType -> dataType));
        }

        return ecoreTypeCache.get(jvmType.getQualifiedName());
    }

    @Inject
    private TypeReferences references;

    private @Nullable JvmType tryGetListType(LightweightTypeReference type) {
        var listType = references.findDeclaredType(List.class, type.getOwner().getContextResourceSet());

        var typeAsList = type.getSuperType(listType);
        if (typeAsList != null) {
            check(typeAsList.getTypeArguments().size() == 1);
            return typeAsList.getTypeArguments().getFirst().getType();
        }

        return null;
    }

    @Inject
    private OnChangeEvictingCache cache;

    public <T> T execUncached(Resource resource, Supplier<T> fun) {
        return cache.execWithTemporaryCaching(resource, res -> fun.get());
    }

}
