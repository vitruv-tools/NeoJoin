package tools.vitruv.optggs.transpiler.operators.patterns;

import tools.vitruv.optggs.operators.FQN;
import tools.vitruv.optggs.transpiler.tgg.Node;
import tools.vitruv.optggs.transpiler.tgg.Slice;

public interface ResolvedPatternLink {
    FQN element();
    Node extendSlice(Slice slice, Node lastNode);
}
