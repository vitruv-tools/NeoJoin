package tools.vitruv.neojoin.utils;

public final class IntVar {
    private int value;

    private IntVar(int value) {
        this.value = value;
    }

    static IntVar of(int value) {
        return new IntVar(value);
    }

    public int get() {
        return value;
    }

    public int set(int value) {
        this.value = value;
        return value;
    }

    public int increaseAndGet() {
        return this.value++;
    }

    public int getAndincrease() {
        return ++this.value;
    }
}
