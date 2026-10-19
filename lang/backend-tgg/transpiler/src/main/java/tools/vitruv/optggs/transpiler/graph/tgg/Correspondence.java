package tools.vitruv.optggs.transpiler.graph.tgg;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Correspondence {
    private final TGGNode source;
    private final TGGNode target;
    private boolean green;

    public static Correspondence Green(TGGNode source, TGGNode target) {
        return new Correspondence(source, target, true);
    }

    public static Correspondence Black(TGGNode source, TGGNode target) {
        return new Correspondence(source, target, false);
    }

    public Correspondence makeGreen() {
        green = true;
        return this;
    }

    public Correspondence makeBlack() {
        green = false;
        return this;
    }

    public CorrespondenceType toCorrespondenceType() {
        return new CorrespondenceType(source, target);
    }

    public Correspondence deepCopy(TGGNodeDeepCopyHelper copyHelper) {
        return new Correspondence(
                copyHelper.getCopiedNode(source), copyHelper.getCopiedNode(target), green);
    }

    @Override
    public String toString() {
        return (green ? "++" : "") + source.getId() + "<-->" + target.getId();
    }
}
