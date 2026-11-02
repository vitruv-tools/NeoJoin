package tools.vitruv.optggs.transpiler.graph.tgg;

import tools.vitruv.optggs.operators.FQN;
import tools.vitruv.optggs.transpiler.graph.Attribute;
import tools.vitruv.optggs.transpiler.graph.NameRepository;

import java.util.ArrayList;
import java.util.LinkedHashSet;

public class TGGNodeFixtures {
    public static TGGNode someTGGNode(
            String id,
            FQN type,
            boolean green,
            NameRepository nameRepository,
            ArrayList<TGGLink> links,
            LinkedHashSet<Attribute> attributes) {
        return new TGGNode(id, type, green, nameRepository, links, attributes);
    }
}
