package tools.vitruv.neojoin.utils;

@FunctionalInterface
public interface ThrowingFunction<A, R, E extends Throwable> {

    public R apply(A a) throws E;
}
