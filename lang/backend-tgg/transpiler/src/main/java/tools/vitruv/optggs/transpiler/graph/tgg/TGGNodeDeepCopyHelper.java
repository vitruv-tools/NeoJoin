package tools.vitruv.optggs.transpiler.graph.tgg;

import tools.vitruv.optggs.transpiler.graph.GraphNodeDeepCopyHelper;
import tools.vitruv.optggs.transpiler.graph.NameRepository;

public class TGGNodeDeepCopyHelper extends GraphNodeDeepCopyHelper<TGGNode> {
    public TGGNodeDeepCopyHelper(NameRepository copiedNameRepository) {
        super(copiedNameRepository);
    }
}
