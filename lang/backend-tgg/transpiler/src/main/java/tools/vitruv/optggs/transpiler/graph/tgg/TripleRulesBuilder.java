package tools.vitruv.optggs.transpiler.graph.tgg;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import tools.vitruv.optggs.transpiler.graph.pattern.GraphPattern;

import java.util.ArrayList;

@NoArgsConstructor
@Getter
public class TripleRulesBuilder {
    private final ArrayList<TripleRule> tripleRules = new ArrayList<>();
    private final ArrayList<GraphPattern> constraints = new ArrayList<>();
    private TripleRule latestRule = null;

    @Setter private GraphPathToNode pathToLastNode = null;

    public TripleRule addRule(TripleRule rule) {
        tripleRules.add(rule);
        latestRule = rule;
        return rule;
    }

    public TripleRule addRule() {
        return addRule(new TripleRule());
    }

    public void removeRule(TripleRule rule) {
        tripleRules.remove(rule);
    }

    public void addLinkToPathToLastNode(String link) {
        pathToLastNode.addLinkToPath(link);
    }

    public void addConstraint(GraphPattern constraint) {
        constraints.add(constraint);
    }
}
