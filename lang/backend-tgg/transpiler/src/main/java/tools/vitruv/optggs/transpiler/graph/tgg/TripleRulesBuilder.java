package tools.vitruv.optggs.transpiler.graph.tgg;

import tools.vitruv.optggs.transpiler.graph.pattern.GraphPattern;

import java.util.ArrayList;

public class TripleRulesBuilder {
    private final ArrayList<TripleRule> tripleRules = new ArrayList<>();
    private final ArrayList<GraphPattern> constraints = new ArrayList<>();
    private TripleRule latestRule = null;

    private GraphPathToNode pathToLastNode = null;

    public ArrayList<TripleRule> getTripleRules() {
        return tripleRules;
    }

    public ArrayList<GraphPattern> getConstraints() {
        return constraints;
    }

    public TripleRule getLatestRule() {
        return latestRule;
    }

    public GraphPathToNode getPathToLastNode() {
        return pathToLastNode;
    }

    public void setPathToLastNode(GraphPathToNode pathToLastNode) {
        this.pathToLastNode = pathToLastNode;
    }

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
