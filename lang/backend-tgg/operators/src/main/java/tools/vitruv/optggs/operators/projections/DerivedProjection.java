package tools.vitruv.optggs.operators.projections;

import tools.vitruv.optggs.operators.FunctionInvocation;
import tools.vitruv.optggs.operators.Projection;

public record DerivedProjection(FunctionInvocation function) implements Projection {

    @Override
    public String toString() {
        return "π(" + function + ")";
    }
}
