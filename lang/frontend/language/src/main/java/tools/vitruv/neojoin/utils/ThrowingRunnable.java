package tools.vitruv.neojoin.utils;

@FunctionalInterface
public interface ThrowingRunnable<E extends Throwable> {

    void run() throws E;
}
