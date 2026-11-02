package tools.vitruv.optggs.transpiler.graph.pattern;

import tools.vitruv.optggs.transpiler.graph.AbstractGraphLink;
import tools.vitruv.optggs.transpiler.graph.GraphNodeDeepCopyHelper;

public class PatternLink extends AbstractGraphLink<PatternNode> {
    public PatternLink(String name, PatternNode target) {
        super(name, target);
    }

    public PatternLink copyWithDifferentNames(PatternNodeRenameCopyHelper copyAndRenameHelper) {
        return new PatternLink(this.getName(), copyAndRenameHelper.getCopiedNode(this.getTarget()));
    }

    @Override
    public PatternLink deepCopy(GraphNodeDeepCopyHelper<PatternNode> copyHelper) {
        return new PatternLink(this.getName(), copyHelper.getCopiedNode(this.getTarget()));
    }
}
