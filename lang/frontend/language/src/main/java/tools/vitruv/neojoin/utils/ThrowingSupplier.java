package tools.vitruv.neojoin.utils;

@FunctionalInterface
public interface ThrowingSupplier<V, E extends Throwable> {

    public V supply() throws E;
}
