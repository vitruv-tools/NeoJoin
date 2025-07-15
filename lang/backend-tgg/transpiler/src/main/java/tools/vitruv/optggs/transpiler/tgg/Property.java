package tools.vitruv.optggs.transpiler.tgg;

public interface Property {
    String name();
    String value();
    String toExpression(Node node);
}
