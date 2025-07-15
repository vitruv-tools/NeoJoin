package tools.vitruv.optggs.transpiler.tgg;

public final class Link {
    private final String name;
    private final Node target;
    private boolean isGreen;

    private Link(String name, Node target, boolean isGreen) {
        this.name = name;
        this.target = target;
        this.isGreen = isGreen;
    }

    public static Link Green(String name, Node target) {
        return new Link(name, target, true);
    }

    public static Link Black(String name, Node target) {
        return new Link(name, target, false);
    }

    public String name() {
        return name;
    }

    public Node target() {
        return target;
    }

    public boolean isGreen() {
        return isGreen;
    }

    public void makeGreen() {
        isGreen = true;
    }

    @Override
    public String toString() {
        return (isGreen ? "++" : "") + "-[" + name() + "]->" + target().id();
    }
}
