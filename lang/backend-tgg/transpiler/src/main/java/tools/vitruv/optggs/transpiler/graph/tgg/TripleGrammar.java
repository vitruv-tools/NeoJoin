package tools.vitruv.optggs.transpiler.graph.tgg;

import tools.vitruv.optggs.transpiler.graph.pattern.GraphPattern;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TripleGrammar {
    private final String name;
    private final List<TripleRule> rules;
    private final List<GraphPattern> constraints;
    private final Set<String> sourceMetamodels;
    private final Set<String> targetMetamodels;

    public TripleGrammar(
            String name,
            List<TripleRule> rules,
            List<GraphPattern> constraints,
            Set<String> sourceMetamodels,
            Set<String> targetMetamodels) {
        this.name = name;
        this.rules = rules;
        this.constraints = constraints;
        this.sourceMetamodels = sourceMetamodels;
        this.targetMetamodels = targetMetamodels;
    }

    public String getName() {
        return name;
    }

    public List<TripleRule> getRules() {
        return rules;
    }

    public List<GraphPattern> getConstraints() {
        return constraints;
    }

    public Set<String> getSourceMetamodels() {
        return sourceMetamodels;
    }

    public Set<String> getTargetMetamodels() {
        return targetMetamodels;
    }

    public Set<CorrespondenceType> getCorrespondenceTypes() {
        return rules.stream()
                .map(TripleRule::correspondences)
                .flatMap(Collection::stream)
                .map(Correspondence::toCorrespondenceType)
                .collect(Collectors.toSet());
    }
}
