package tools.vitruv.neojoin.utils;

@FunctionalInterface
public interface ThrowingConsumer<A, E extends Throwable> {

    public void accept(A a) throws E;
}
