package tools.vitruv.optggs.transpiler.operators.patterns;

import tools.vitruv.optggs.operators.FQN;
import tools.vitruv.optggs.transpiler.graph.tgg.TGGNode;
import tools.vitruv.optggs.transpiler.graph.tgg.TGGSlice;

public interface ResolvedPatternLink {
    FQN element();
    TGGNode extendSlice(TGGSlice slice, TGGNode lastNode);
}
