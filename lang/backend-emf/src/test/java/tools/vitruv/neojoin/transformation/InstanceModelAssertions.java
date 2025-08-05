package tools.vitruv.neojoin.transformation;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.jspecify.annotations.Nullable;
import tools.vitruv.neojoin.utils.Utils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class InstanceModelAssertions extends AbstractAssert<InstanceModelAssertions, EObject> {

	private final Set<EObject> seenInstances = new HashSet<>();

	protected InstanceModelAssertions(EObject actual) {
		super(actual, InstanceModelAssertions.class);
	}

	public static InstanceModelAssertions assertThat(EObject actual) {
		return new InstanceModelAssertions(actual);
	}

	private Stream<EObject> getContents(boolean includeRoot) {
		var result = Utils.streamOf(EcoreUtil.<EObject>getAllContents(actual, true));
		if (includeRoot) {
			result = Stream.concat(Stream.of(actual), result);
		}
		return result;
	}

	private Stream<EObject> getContentsOfClass(String clazz) {
		return getContents(true).filter(obj -> obj.eClass().getName().equals(clazz));
	}

	public InstanceModelAssertions hasInstance(String clazz, Predicate<EObject> filter, Consumer<EObject> consumer) {
		isNotNull();

		var results = getContentsOfClass(clazz).filter(filter).toList();
		Assertions.assertThat(results).as(
			"Expected to find 1 instance of class %s matching criteria %s, but found %d",
			clazz,
			filter,
			results.size()
		).hasSize(1);
		var instance = results.getFirst();
		Assertions.assertThat(seenInstances)
			.as("Matching instance was already checked before: %s", instance)
			.doesNotContain(instance);
		consumer.accept(instance);
		seenInstances.add(instance);
		return this;
	}

	public InstanceModelAssertions hasInstance(String clazz, Predicate<EObject> filter) {
		return hasInstance(clazz, filter, obj -> {});
	}

	public void hasNoMoreInstances() {
		hasNoMoreInstances(false);
	}

	public void hasNoMoreInstances(boolean includeRoot) {
		isNotNull();
		var remaining = getContents(includeRoot).filter(c -> !seenInstances.contains(c)).toList();
		Assertions.assertThat(remaining).isEmpty();
	}

	public void hasNoMoreInstancesOfClass(String clazz) {
		isNotNull();
		var remaining = getContentsOfClass(clazz).filter(c -> !seenInstances.contains(c)).toList();
		Assertions.assertThat(remaining).isEmpty();
	}

	public EStructuralFeature getFeature(String name) {
		isNotNull();
		var feature = actual.eClass().getEStructuralFeature(name);
		Assertions.assertThat(feature).isNotNull();
		return feature;
	}

	public InstanceModelAssertions hasAttribute(String name, @Nullable Object value) {
		var feature = getFeature(name);
		Assertions.assertThat(feature).isInstanceOf(EAttribute.class);
		var actualValue = actual.eGet(feature);
		if (actualValue instanceof EEnumLiteral enumLiteral) {
			// compare enum literals by name because testing code cannot reference enum literals of the generated meta-model
			actualValue = enumLiteral.getName();
		}
		Assertions.assertThat(actualValue).isEqualTo(value);
		return this;
	}

	private static void checkReferenceValue(@Nullable Object value) {
		if (value != null) {
			Assertions.assertThat(value).isInstanceOf(EObject.class);
		}
	}

	public InstanceModelAssertions hasReference(String name, Consumer<@Nullable Object> consumer) {
		var feature = getFeature(name);
		Assertions.assertThat(feature).isInstanceOf(EReference.class);
		var value = actual.eGet(feature);
		if (feature.isMany()) {
			Assertions.assertThat(value).isInstanceOfSatisfying(
				EList.class, elist -> {
					//noinspection unchecked
					elist.forEach(InstanceModelAssertions::checkReferenceValue);
				}
			);
		} else {
			checkReferenceValue(value);
		}
		consumer.accept(value);
		return this;
	}

	public InstanceModelAssertions hasReference(String name, Predicate<@Nullable EObject> filter) {
		return hasReference(
			name, obj -> {
				Assertions.assertThat(filter.test((EObject) obj)).isTrue();
			}
		);
	}

	public InstanceModelAssertions hasManyReference(String name, Consumer<List<EObject>> consumer) {
		return hasReference(
			name, obj -> {
				//noinspection DataFlowIssue - false positive
				Assertions.assertThat(obj).isInstanceOfSatisfying(List.class, consumer::accept);
			}
		);
	}

	@SafeVarargs
	public final InstanceModelAssertions hasManyReference(String name, Predicate<EObject>... filters) {
		return hasManyReference(
			name, list -> {
				Assertions.assertThat(list).hasSize(filters.length);
				var index = 0;
				for (var filter : filters) {
					var filtered = list.stream().filter(filter).toList();
					Assertions.assertThat(filtered)
						.as("Expected filter %d to match exactly one element: %s", index, filter)
						.hasSize(1);
					index++;
				}
			}
		);
	}

}
