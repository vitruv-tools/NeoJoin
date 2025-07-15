package tools.vitruv.optggs.operators.builder;

import tools.vitruv.optggs.operators.FQN;
import tools.vitruv.optggs.operators.FunctionInvocation;
import tools.vitruv.optggs.operators.Tuple;
import tools.vitruv.optggs.operators.Query;
import tools.vitruv.optggs.operators.Selection;
import tools.vitruv.optggs.operators.selection.Pattern;

import java.util.Collection;

public class TargetSelectionBuilder {
    private final Pattern source;
    private final Pattern pattern;

    public TargetSelectionBuilder(Pattern source, FQN element) {
        this.source = source;
        this.pattern = Pattern.from(element);
    }

    private TargetSelectionBuilder(Pattern source, Pattern pattern) {
        this.source = source;
        this.pattern = pattern;
    }

    public TargetSelectionBuilder join(FQN element, Collection<Tuple<String, String>> properties) {
        return new TargetSelectionBuilder(source, pattern.join(element, properties));
    }

    public TargetSelectionBuilder join(FQN element, String originProperty, String destinationProperty) {
        return new TargetSelectionBuilder(source, pattern.join(element, originProperty, destinationProperty));
    }

    public TargetSelectionBuilder join(FQN element, String property) {
        return new TargetSelectionBuilder(source, pattern.join(element, property));
    }

    public TargetSelectionBuilder ref(FQN element, String reference) {
        return new TargetSelectionBuilder(source, pattern.ref(element, reference));
    }

    public TargetSelectionBuilder join(FQN element, FunctionInvocation functionInvocation) {
        return new TargetSelectionBuilder(source, pattern.join(element, functionInvocation));
    }

    public Query build() {
        return new Query(new Selection(source, pattern));
    }
}
