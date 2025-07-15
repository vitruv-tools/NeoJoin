package tools.vitruv.optggs.transpiler.operators;

import org.junit.jupiter.api.Test;
import tools.vitruv.optggs.operators.FQN;
import tools.vitruv.optggs.transpiler.TranspilerQueryResolver;
import tools.vitruv.optggs.transpiler.operators.patterns.ResolvedPattern;
import tools.vitruv.optggs.operators.selection.Pattern;
import tools.vitruv.optggs.transpiler.tgg.TripleRule;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResolvedLinkTest {
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
    public void extendRule() {
        var source = resolve(Pattern.from(pkg("A")).ref(pkg("B"), "b"));
        var target = resolve(Pattern.from(t("A'")).ref(t("B'"), "b"));
        var link = new ResolvedLink(source, target, List.of());

        var rule = new TripleRule();
        rule.addSourceSlice().addNode(pkg("A"));
        rule.addTargetSlice().addNode(t("A'"));
        assertEquals("src: [<a: pkg.A;;>] tgt: [<a': t.A';;>] corr: []", rule.toString());
        link.extendRule(rule);
        assertEquals("src: [<a: pkg.A;++-[b]->b;>, <b: pkg.B;;>] tgt: [<a': t.A';++-[b]->b';>, <b': t.B';;>] corr: [b<-->b']", rule.toString());
    }

    @Test
    public void extendComplexRule() {
        var source = resolve(Pattern.from(pkg("A")).ref(pkg("B"), "b").ref(pkg("C"), "c"));
        var target = resolve(Pattern.from(t("A'")).ref(t("B'"), "b"));
        var link = new ResolvedLink(source, target, List.of());

        var rule = new TripleRule();
        rule.addSourceSlice().addNode(pkg("A"));
        rule.addTargetSlice().addNode(t("A'"));
        assertEquals("src: [<a: pkg.A;;>] tgt: [<a': t.A';;>] corr: []", rule.toString());
        link.extendRule(rule);
        assertEquals("src: [<a: pkg.A;++-[b]->b;>, <++b: pkg.B;++-[c]->c;>, <c: pkg.C;;>] tgt: [<a': t.A';++-[b]->b';>, <b': t.B';;>] corr: [c<-->b']", rule.toString());
    }
}