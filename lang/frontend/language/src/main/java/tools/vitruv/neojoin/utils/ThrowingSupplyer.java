package tools.vitruv.neojoin.utils;

@FunctionalInterface
public interface ThrowingSupplyer<V, E extends Throwable> {

    public V invoke() throws E;
}
