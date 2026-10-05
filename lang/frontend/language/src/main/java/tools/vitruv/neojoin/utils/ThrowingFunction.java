package tools.vitruv.neojoin.utils;

@FunctionalInterface
public interface ThrowingFunction<A, R, E extends Throwable> {

    public R invoke(A a) throws E;
}
