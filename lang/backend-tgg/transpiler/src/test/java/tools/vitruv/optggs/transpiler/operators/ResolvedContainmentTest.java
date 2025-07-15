package tools.vitruv.optggs.transpiler.operators;

import org.junit.jupiter.api.Test;
import tools.vitruv.optggs.operators.FQN;
import tools.vitruv.optggs.transpiler.TranspilerQueryResolver;
import tools.vitruv.optggs.transpiler.operators.patterns.ResolvedPattern;
import tools.vitruv.optggs.operators.selection.Pattern;
import tools.vitruv.optggs.transpiler.tgg.TripleRule;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResolvedContainmentTest {

    private static FQN pkg(String name) {
        return new FQN("pkg", name);
    }

    private static FQN t(String name) {
        return new FQN("t", name);
    }

    private ResolvedPattern resolve(Pattern pattern) {
        return new TranspilerQueryResolver().resolvePattern(pattern);
    }


    @Test
    public void toRule() {
        var source = resolve(Pattern.from(pkg("A")).ref(pkg("B"), "b"));
        var target = resolve(Pattern.from(t("A'")).ref(t("B'"), "b"));
        var container = new ResolvedContainment(source, target, List.of());
        var rule = new TripleRule();
        rule.addSourceSlice().addNode(pkg("B")).makeGreen();
        rule.addTargetSlice().addNode(t("B'")).makeGreen();
        assertEquals("src: [<++b: pkg.B;;>] tgt: [<++b': t.B';;>] corr: []", rule.toString());
        container.extendRule(rule);
        System.out.println(rule);
        assertEquals("src: [<++b: pkg.B;;>, <a: pkg.A;++-[b]->b;>] tgt: [<++b': t.B';;>, <a': t.A';++-[b]->b';>] corr: [a<-->a']", rule.toString());
    }

}