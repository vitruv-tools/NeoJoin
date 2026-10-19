package tools.vitruv.optggs.transpiler.graph.tgg;

import lombok.Getter;

import tools.vitruv.optggs.transpiler.graph.AbstractGraphLink;
import tools.vitruv.optggs.transpiler.graph.GraphNodeDeepCopyHelper;
import tools.vitruv.optggs.transpiler.graph.pattern.PatternLink;

@Getter
public class TGGLink extends AbstractGraphLink<TGGNode> {
    boolean green;

    private TGGLink(String name, TGGNode target, boolean green) {
        super(name, target);
        this.green = green;
    }

    public static TGGLink Green(String name, TGGNode target) {
        return new TGGLink(name, target, true);
    }

    public static TGGLink Black(String name, TGGNode target) {
        return new TGGLink(name, target, false);
    }

    public TGGLink makeGreen() {
        this.green = true;
        return this;
    }

    public TGGLink makeBlack() {
        this.green = false;
        return this;
    }

    @Override
    public TGGLink deepCopy(GraphNodeDeepCopyHelper<TGGNode> copyHelper) {
        return new TGGLink(this.getName(), copyHelper.getCopiedNode(this.getTarget()), green);
    }

    public PatternLink toPatternLink(TGGNodeToPatternNodeConversionHelper conversionHelper) {
        return new PatternLink(name, conversionHelper.getConvertedNode(target));
    }

    @Override
    public String toString() {
        return (green ? "++" : "") + "-[" + name + "]->" + target.getId();
    }
}
