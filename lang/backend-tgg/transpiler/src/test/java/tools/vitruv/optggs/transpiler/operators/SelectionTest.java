package tools.vitruv.optggs.transpiler.operators;

import org.junit.jupiter.api.Test;
import tools.vitruv.optggs.operators.FQN;
import tools.vitruv.optggs.operators.Mapping;
import tools.vitruv.optggs.operators.Selection;
import tools.vitruv.optggs.transpiler.TranspilerQueryResolver;
import tools.vitruv.optggs.operators.selection.Pattern;
import tools.vitruv.optggs.transpiler.tgg.TripleRule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SelectionTest {

    private static FQN pkg(String name) {
        return new FQN("pkg", name);
    }

    private static FQN t(String name) {
        return new FQN("t", name);
    }

    private ResolvedSelection resolve(Selection selection) {
        return new TranspilerQueryResolver().resolveSelection(selection);
    }

    @Test
    public void simpleMapping() {
        var selection = new Selection(Pattern.from(pkg("A")), Pattern.from(t("A'")));
        var mappings = selection.mappings();
        assertEquals(1, mappings.size());
        assertEquals("[pkg.A => t.A']", mappings.toString());
        var primaryMappings = selection.primaryMappings();
        assertEquals(1, primaryMappings.size());
        assertEquals("[pkg.A => t.A']", primaryMappings.toString());
    }

    @Test
    public void complexMapping() {
        var selection = new Selection(
                Pattern.from(pkg("A")).ref(pkg("B"), "b"),
                Pattern.from(t("A'")).ref(t("B'"), "b")
        );
        var mappings = selection.mappings();
        assertEquals(4, mappings.size());
        assertTrue(mappings.contains(new Mapping(pkg("A"), t("A'"))));
        assertTrue(mappings.contains(new Mapping(pkg("A"), t("B'"))));
        assertTrue(mappings.contains(new Mapping(pkg("B"), t("A'"))));
        assertTrue(mappings.contains(new Mapping(pkg("B"), t("B'"))));
        var primaryMappings = selection.primaryMappings();
        assertEquals(4, primaryMappings.size());
        assertTrue(primaryMappings.contains(new Mapping(pkg("A"), t("A'"))));
        assertTrue(primaryMappings.contains(new Mapping(pkg("A"), t("B'"))));
        assertTrue(primaryMappings.contains(new Mapping(pkg("B"), t("A'"))));
        assertTrue(primaryMappings.contains(new Mapping(pkg("B"), t("B'"))));
    }

    @Test
    public void veryComplexMapping() {
        var selection = new Selection(
                Pattern.from(pkg("A")).ref(pkg("C"), "c").ref(pkg("B"), "b"),
                Pattern.from(t("A'")).ref(t("B'"), "b")
        );
        var mappings = selection.mappings();
        assertEquals(6, mappings.size());
        assertTrue(mappings.contains(new Mapping(pkg("A"), t("A'"))));
        assertTrue(mappings.contains(new Mapping(pkg("A"), t("B'"))));
        assertTrue(mappings.contains(new Mapping(pkg("B"), t("A'"))));
        assertTrue(mappings.contains(new Mapping(pkg("B"), t("B'"))));
        assertTrue(mappings.contains(new Mapping(pkg("C"), t("A'"))));
        assertTrue(mappings.contains(new Mapping(pkg("C"), t("B'"))));
        var primaryMappings = selection.primaryMappings();
        assertEquals(4, primaryMappings.size());
        assertTrue(primaryMappings.contains(new Mapping(pkg("A"), t("A'"))));
        assertTrue(primaryMappings.contains(new Mapping(pkg("A"), t("B'"))));
        assertTrue(primaryMappings.contains(new Mapping(pkg("B"), t("A'"))));
        assertTrue(primaryMappings.contains(new Mapping(pkg("B"), t("B'"))));
    }

    @Test
    public void simpleMappingToRule() {
        var selection = resolve(new Selection(Pattern.from(pkg("A")), Pattern.from(t("A'"))));
        var rule = new TripleRule();
        selection.extendRule(rule);
        assertEquals("src: [<a: pkg.A;;>] tgt: [<a': t.A';;>] corr: [a<-->a']", rule.toString());
    }

    @Test
    public void complexMappingToPattern() {
        var selection = resolve(new Selection(
                Pattern.from(pkg("A")).ref(pkg("B"), "b"),
                Pattern.from(t("A'")).ref(t("B'"), "b")
        ));
        var rule = new TripleRule();
        selection.extendRule(rule);
        assertEquals("src: [<a: pkg.A;-[b]->b;>, <b: pkg.B;;>] tgt: [<a': t.A';-[b]->b';>, <b': t.B';;>] corr: [a<-->a', a<-->b', b<-->a', b<-->b']", rule.toString());
    }

    @Test
    public void veryComplexMappingToPattern() {
        var selection = resolve(new Selection(
                Pattern.from(pkg("A")).ref(pkg("C"), "c").ref(pkg("B"), "b"),
                Pattern.from(t("A'")).ref(t("B'"), "b")
        ));
        var rule = new TripleRule();
        selection.extendRule(rule);
        assertEquals("src: [<a: pkg.A;-[c]->c;>, <c: pkg.C;-[b]->b;>, <b: pkg.B;;>] tgt: [<a': t.A';-[b]->b';>, <b': t.B';;>] corr: [a<-->a', a<-->b', b<-->a', c<-->a', b<-->b', c<-->b']", rule.toString());
    }

    @Test
    public void multiJoinToPattern() {
        var selection = resolve(new Selection(
                Pattern.from(pkg("A")).join(pkg("B"), "id").join(pkg("C"), "id"),
                Pattern.from(t("A'"))
        ));
        var rule = new TripleRule();
        selection.extendRule(rule);
        assertEquals("src: [<a: pkg.A;;.id==<id>>, <b: pkg.B;;.id==<id>>, <c: pkg.C;;.id==<id>>] tgt: [<a': t.A';;>] corr: [a<-->a', b<-->a', c<-->a']", rule.toString());
    }

}