package tools.vitruv.optggs.transpiler.tgg;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public class TripleGrammar {
    private final String name;
    private final Collection<TripleRule> rules;
    private final Set<String> sourceMetamodels;
    private final Set<String> targetMetamodels;

    public TripleGrammar(String name, Set<String> sourceMetamodels, Set<String> targetMetamodels, Collection<TripleRule> rules) {
        this.name = name;
        this.sourceMetamodels = sourceMetamodels;
        this.targetMetamodels = targetMetamodels;
        this.rules = rules;
    }

    public String name() {
        return name;
    }

    public Collection<TripleRule> rules() {
        return rules;
    }

    public Set<CorrespondenceType> correspondenceTypes() {
        return rules.stream()
                .map(TripleRule::correspondences)
                .flatMap(Collection::stream)
                .map(Correspondence::toCorrespondenceType)
                .collect(Collectors.toSet());
    }

    public Set<String> sourceMetamodels() {
        return sourceMetamodels;
    }

    public Set<String> targetMetamodels() {
        return targetMetamodels;
    }
}
