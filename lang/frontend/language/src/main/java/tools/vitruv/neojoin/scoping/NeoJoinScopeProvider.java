package tools.vitruv.neojoin.scoping;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.xtext.naming.IQualifiedNameConverter;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.resource.EObjectDescription;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.scoping.IScope;
import org.eclipse.xtext.scoping.impl.SimpleScope;
import tools.vitruv.neojoin.Constants;
import tools.vitruv.neojoin.ast.AstPackage;
import tools.vitruv.neojoin.ast.From;
import tools.vitruv.neojoin.ast.Join;
import tools.vitruv.neojoin.ast.JoinFeatureCondition;
import tools.vitruv.neojoin.ast.Source;
import tools.vitruv.neojoin.ast.ViewTypeDefinition;
import tools.vitruv.neojoin.jvmmodel.ExpressionHelper;
import tools.vitruv.neojoin.utils.AstUtils;
import tools.vitruv.neojoin.utils.EMFUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static tools.vitruv.neojoin.utils.Assertions.check;
import static tools.vitruv.neojoin.utils.Assertions.fail;

/**
 * Defines which elements can be referenced in various positions. See <a href="https://eclipse.dev/Xtext/documentation/303_runtime_concepts.html#scoping">here</a>.
 */
public class NeoJoinScopeProvider extends AbstractNeoJoinScopeProvider {

	@Inject
	@Named(Constants.ImportPackageRegistry)
	private EPackage.Registry packageRegistry;

	@Inject
	private IQualifiedNameConverter qualifiedNameConverter;

	@Inject
	private ExpressionHelper expressionHelper;

	@Override
	public IScope getScope(EObject context, EReference reference) {
		if (reference == AstPackage.Literals.IMPORT__PACKAGE) {
			return createAvailableModelsScope();
		} else if (reference == AstPackage.Literals.FROM__CLAZZ) {
			return createImportedClassifiersScope(AstUtils.getViewType(context));
		} else if (reference == AstPackage.Literals.JOIN_FEATURE_CONDITION__OTHER) {
			var join = (Join) context.eContainer();
			var source = (Source) join.eContainer();
			var from = join.getFrom();
			if (source != null && from != null) {
				return createJoinConditionOtherScope(source, from);
			}
		} else if (reference == AstPackage.Literals.JOIN_FEATURE_CONDITION__FEATURES) {
			var condition = (JoinFeatureCondition) context;
			var join = (Join) condition.eContainer();
			EClass left;
			if (condition.getOther() != null) {
				left = condition.getOther().getClazz();
			} else {
				var source = (Source) join.eContainer();
				left = source.getFrom().getClazz();
			}
			var right = join.getFrom().getClazz();
			if (left != null && right != null) {
				return createJoinConditionFieldsScope(left, right);
			}
		} else if (reference == AstPackage.Literals.FEATURE__TYPE) {
			return createFeatureTypeScope(AstUtils.getViewType(context));
		} else {
			return super.getScope(context, reference);
		}

		return IScope.NULLSCOPE;
	}

	/**
	 * Scope for all models available for import.
	 */
	private IScope createAvailableModelsScope() {
		var candidates = EMFUtils.collectAvailablePackages(packageRegistry).stream()
			.map(pack -> EObjectDescription.create(qualifiedNameConverter.toQualifiedName(pack.getNsURI()), pack))
			.toList();
		return new SimpleScope(IScope.NULLSCOPE, candidates);
	}

	/**
	 * Scope for all classes that can be referenced by queries.
	 */
	private IScope createImportedClassifiersScope(ViewTypeDefinition viewType) {
		var imports = AstUtils.getImportedPackagesByAlias(viewType);
		return createImportedClassifierScope(imports, c -> c instanceof EClass);
	}

	/**
	 * Scope for the other class to compare with in a join feature condition (e.g. {@code join ... with other using attr})
	 */
	private IScope createJoinConditionOtherScope(Source source, From from) {
		var candidates = AstUtils.getAllFroms(source)
			.takeWhile(f -> f != from)
			.filter(f -> f.getAlias() != null)
			.map(f -> EObjectDescription.create(f.getAlias(), f))
			.toList();
		return new SimpleScope(IScope.NULLSCOPE, candidates);
	}

	/**
	 * Scope for the common and compatible features of two classes for usage in a feature join condition.
	 */
	private IScope createJoinConditionFieldsScope(EClass left, EClass right) {
		var candidates = left.getEAllStructuralFeatures().stream()
			.filter(leftFeature -> {
				var rightFeature = right.getEStructuralFeature(leftFeature.getName());
				return switch (leftFeature) {
					case EAttribute leftAttr ->
						rightFeature instanceof EAttribute rightAttr && leftAttr.getEAttributeType() == rightAttr.getEAttributeType();
					case EReference leftRef ->
						rightFeature instanceof EReference rightRef && leftRef.getEReferenceType() == rightRef.getEReferenceType();
					default -> fail();
				};
			})
			.map(feature -> EObjectDescription.create(feature.getName(), feature))
			.toList();
		return new SimpleScope(IScope.NULLSCOPE, candidates);
	}

	/**
	 * Scope for available types when specifying an explicit type for a feature.
	 */
	private IScope createFeatureTypeScope(ViewTypeDefinition viewType) {
		var imports = AstUtils.getImportedPackagesByAlias(viewType);
		imports.put(Constants.EcoreAlias, EcorePackage.eINSTANCE); // ecore is always available
		var dataTypeScope = createImportedClassifierScope(imports, c -> c instanceof EDataType);

		var queryCandidates = AstUtils.getAllQueries(viewType)
			.map(query -> EObjectDescription.create(AstUtils.getTargetName(query, expressionHelper), query))
			.toList();

		return new SimpleScope(dataTypeScope, queryCandidates);
	}

	/**
	 * Creates a scope for all imported classifiers that match the given filter.
	 */
	private IScope createImportedClassifierScope(Map<String, EPackage> imports, Predicate<EClassifier> filter) {
		var candidates = imports.entrySet().stream()
			.flatMap(entry -> EMFUtils.getAllEClassifiers(entry.getValue())
				.filter(filter)
				.flatMap(c -> {
					var unqualifiedName = QualifiedName.create(c.getName());
					var qualifiedName = getFullyQualifiedName(c, entry.getKey());
					return Stream.of(
						EObjectDescription.create(unqualifiedName, c),
						EObjectDescription.create(qualifiedName, c)
					);
				})
			);

		return new SimpleScope(IScope.NULLSCOPE, filterCollisions(candidates));
	}

	/**
	 * Returns the fully qualified name of the given classifier. The first part of the fully qualified name is
	 * the given alias of the root package or its name if no alias is specified.
	 *
	 * @param classifier classifier to get the fully qualified name for
	 * @param rootAlias  alias to use as the first part of the fully qualified name (e.g. the root package alias)
	 * @return fully qualified name of the classifier
	 */
	private static QualifiedName getFullyQualifiedName(EClassifier classifier, String rootAlias) {
		var parts = Stream.concat(
			EMFUtils.getFullyQualifiedNameAsStream(classifier.getEPackage()),
			Stream.of(classifier.getName())
		).toArray(String[]::new);
		check(parts.length > 1); // at least package name + class name
		parts[0] = rootAlias;
		return QualifiedName.create(parts);
	}

	/**
	 * Filters name collisions from the given stream of {@link IEObjectDescription object descriptions}.
	 *
	 * @param descriptions stream of object descriptions
	 * @return subset of the given object descriptions with distinct names
	 */
	private static List<IEObjectDescription> filterCollisions(Stream<IEObjectDescription> descriptions) {
		return descriptions
			.collect(Collectors.groupingBy(IEObjectDescription::getName, Collectors.toList()))
			.values().stream()
			.filter(collisions -> collisions.size() == 1)
			.map(List::getFirst)
			.toList();
	}

}
