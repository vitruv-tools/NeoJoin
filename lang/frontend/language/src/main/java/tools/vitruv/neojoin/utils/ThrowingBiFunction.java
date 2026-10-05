package tools.vitruv.neojoin.utils;

@FunctionalInterface
public interface ThrowingBiFunction<A, B, R, E extends Throwable> {

    public R invoke(A a, B b) throws E;
}
