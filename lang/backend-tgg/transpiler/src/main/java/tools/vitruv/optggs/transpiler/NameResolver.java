package tools.vitruv.optggs.transpiler;

import tools.vitruv.optggs.operators.FQN;
import tools.vitruv.optggs.transpiler.graph.pattern.GraphPattern;
import tools.vitruv.optggs.transpiler.graph.tgg.Correspondence;
import tools.vitruv.optggs.transpiler.graph.tgg.CorrespondenceType;
import tools.vitruv.optggs.transpiler.graph.tgg.TGGNode;
import tools.vitruv.optggs.transpiler.graph.tgg.TripleRule;

import java.util.UUID;

public interface NameResolver {
    String resolveName(FQN fqn);

    default String resolveCorrespondenceName(CorrespondenceType correspondence) {
        return correspondence.source().localName() + "To" + correspondence.target().localName();
    }

    default String resolveCorrespondenceName(Correspondence correspondence) {
        return resolveCorrespondenceName(correspondence.toCorrespondenceType());
    }

    default String resolveRuleName(TripleRule rule) {
        var greenSources = rule.allSourcesAsSlice().findNodes(TGGNode::isGreen);
        var greenTargets = rule.allTargetsAsSlice().findNodes(TGGNode::isGreen);
        var sources = greenSources.stream().map(node -> node.getType().localName()).toList();
        var targets = greenTargets.stream().map(node -> node.getType().localName()).toList();
        return "Transform" + String.join("And", sources)
                + "To" + String.join("And", targets)
                + toAlphanumeric(rule.getId());
    }

    default String resolvePatternName(GraphPattern pattern) {
        return "Pattern" + toAlphanumeric(pattern.getId());
    }

    default String resolveConstraintName(GraphPattern pattern) {
        return "No" + resolvePatternName(pattern);
    }

    private static String toAlphanumeric(UUID id) {
        return id.toString().replace("-", "");
    }
}
