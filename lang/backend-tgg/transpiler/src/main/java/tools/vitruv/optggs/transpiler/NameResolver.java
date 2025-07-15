package tools.vitruv.optggs.transpiler;

import tools.vitruv.optggs.operators.FQN;
import tools.vitruv.optggs.transpiler.tgg.CorrespondenceType;
import tools.vitruv.optggs.transpiler.tgg.Correspondence;
import tools.vitruv.optggs.transpiler.tgg.Node;
import tools.vitruv.optggs.transpiler.tgg.TripleRule;

public interface NameResolver {
    String resolveName(FQN fqn);

    default String resolveCorrespondenceName(CorrespondenceType correspondence) {
        return correspondence.source().localName() + "To" + correspondence.target().localName();
    }

    default String resolveCorrespondenceName(Correspondence correspondence) {
        return resolveCorrespondenceName(correspondence.toCorrespondenceType());
    }

    default String resolveRuleName(TripleRule rule) {
        if (rule.isLinkRule()) {
            // Link rule
            var originNodes = rule.allSourcesAsSlice().filterMapNodes(node -> !node.links().isEmpty(), node -> node.type().localName());
            var destinationNodes = rule.allSourcesAsSlice().filterMapNodes(node -> node.links().isEmpty(), node -> node.type().localName());
            return "Link" + String.join("And", originNodes) + "To" + String.join("And", destinationNodes);
        } else {
            // Select rule
            var greenSources = rule.allSourcesAsSlice().findNodes(Node::isGreen);
            var greenTargets = rule.allTargetsAsSlice().findNodes(Node::isGreen);
            var sources = greenSources.stream().map(node -> node.type().localName()).toList();
            var targets = greenTargets.stream().map(node -> node.type().localName()).toList();
            return "Select" + String.join("And", sources)
                    + "As" + String.join("And", targets);
        }
    }
}
