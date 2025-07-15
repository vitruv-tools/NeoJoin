package tools.vitruv.optggs.operators.projections;

import tools.vitruv.optggs.operators.FQN;
import tools.vitruv.optggs.operators.Containment;
import tools.vitruv.optggs.operators.Projection;
import tools.vitruv.optggs.operators.selection.Pattern;

import java.util.Collection;

public record SimpleProjection(Pattern source,
                               FQN target,
                               String sourceProperty,
                               String targetProperty) implements Projection {

    public boolean isContained(Collection<Containment> containments, Pattern pattern) {
        return containments.stream().anyMatch((containment) -> containment.sources().anyStartWith(pattern));
    }

    @Override
    public String toString() {
        return "π(" + source + "::" + sourceProperty + " => " + target.fqn() + "::" + targetProperty + ")";
    }
}
