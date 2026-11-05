package tools.vitruv.neojoin.utils;

import org.eclipse.emf.common.notify.impl.AdapterImpl;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.jspecify.annotations.Nullable;
import tools.vitruv.neojoin.jvmmodel.SourceModelInferrer;

import static tools.vitruv.neojoin.utils.Assertions.require;

/**
 * Can be used to store arbitrary values within {@link EObject EObjects}. See {@code EcoreSourceHolder} within
 * {@link SourceModelInferrer} for a usage example.
 *
 * @param <T> type of the value
 */
public abstract class EValueHolder<T> extends AdapterImpl {

    private final T value;

    protected EValueHolder(T value) {
        this.value = value;
    }

    public T getValue() {
        return value;
    }

    @Override
    public boolean isAdapterForType(Object type) {
        return getClass() == type;
    }

    protected static <T> void install(EObject object, EValueHolder<T> holder) {
        require(EcoreUtil2.getExistingAdapter(object, holder.getClass()) == null, "Value already set");
        object.eAdapters().add(holder);
    }

    protected static <T> T retrieve(EObject object, Class<? extends EValueHolder<T>> type) {
        var adapter = EcoreUtil2.getExistingAdapter(object, type);
        require(adapter != null, "No value set");
        return type.cast(adapter).getValue();
    }

    protected static <T> @Nullable T retrieveOrNull(EObject object, Class<? extends EValueHolder<T>> type) {
        var adapter = EcoreUtil2.getExistingAdapter(object, type);
        return adapter == null ? null : type.cast(adapter).getValue();
    }

}
