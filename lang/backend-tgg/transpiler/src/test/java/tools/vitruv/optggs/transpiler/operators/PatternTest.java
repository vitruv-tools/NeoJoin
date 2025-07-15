package tools.vitruv.optggs.transpiler.operators;

import org.junit.jupiter.api.Test;
import tools.vitruv.optggs.operators.FQN;
import tools.vitruv.optggs.transpiler.TranspilerQueryResolver;
import tools.vitruv.optggs.operators.Tuple;
import tools.vitruv.optggs.transpiler.operators.patterns.ResolvedPattern;
import tools.vitruv.optggs.operators.selection.Pattern;
import tools.vitruv.optggs.operators.selection.From;
import tools.vitruv.optggs.operators.selection.Join;
import tools.vitruv.optggs.transpiler.tgg.TripleRule;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PatternTest {

    private static FQN pkg(String name) {
        return new FQN("pkg", name);
    }

    private ResolvedPattern resolve(Pattern pattern) {
        return new TranspilerQueryResolver().resolvePattern(pattern);
    }

    @Test
    public void testFrom() {
        var pattern = Pattern.from(pkg("A"));
        assertEquals("pkg.A", pattern.toString());
        assertEquals("pkg.A", pattern.top().fqn());
        assertEquals("pkg.A", pattern.bottom().fqn());
    }

    @Test
    public void testSimpleJoin() {
        var pattern = Pattern.from(pkg("A")).join(pkg("B"), "id");
        assertEquals("pkg.A ⨝(id==id) pkg.B", pattern.toString());
        assertEquals("pkg.A", pattern.top().fqn());
        assertEquals("pkg.B", pattern.bottom().fqn());
    }

    @Test
    public void testMultiJoin() {
        var pattern = Pattern.from(pkg("A")).join(pkg("B"), List.of(new Tuple<>("id", "id"), new Tuple<>("name", "name")));
        assertEquals("pkg.A ⨝(id==id,name==name) pkg.B", pattern.toString());
        assertEquals("pkg.A", pattern.top().fqn());
        assertEquals("pkg.B", pattern.bottom().fqn());
    }

    @Test
    public void testRef() {
        var pattern = Pattern.from(pkg("A")).ref(pkg("B"), "child");
        assertEquals("pkg.A-[child]->pkg.B", pattern.toString());
        assertEquals("pkg.A", pattern.top().fqn());
        assertEquals("pkg.B", pattern.bottom().fqn());
    }

    @Test
    public void testComplex() {
        var pattern = Pattern.from(pkg("A")).ref(pkg("B"), "child").join(pkg("C"), "id");
        assertEquals("pkg.A-[child]->pkg.B ⨝(id==id) pkg.C", pattern.toString());
        assertEquals("pkg.A", pattern.top().fqn());
        assertEquals("pkg.C", pattern.bottom().fqn());
    }

    @Test
    public void toRule() {
        var pattern = Pattern.from(pkg("A")).ref(pkg("B"), "child").join(pkg("C"), "id");
        var rule = new TripleRule();
        var slice = rule.addSourceSlice();
        resolve(pattern).extendSlice(slice);
        assertEquals("[<a: pkg.A;-[child]->b;>, <b: pkg.B;;.id==<id>>, <c: pkg.C;;.id==<id>>]", slice.toString());
        assertEquals("src: [<a: pkg.A;-[child]->b;>, <b: pkg.B;;.id==<id>>, <c: pkg.C;;.id==<id>>] tgt: [] corr: []", rule.toString());
    }

    @Test
    public void popTop() {
        var pattern = Pattern.from(pkg("A")).ref(pkg("B"), "child").join(pkg("C"), "id");
        var tuple = pattern.popTop();
        assertEquals(new From(pkg("A")), tuple.last());
        assertEquals("-[child]->pkg.B ⨝(id==id) pkg.C", tuple.first().toString());
    }

    @Test
    public void popBottom() {
        var pattern = Pattern.from(pkg("A")).ref(pkg("B"), "child").join(pkg("C"), "id");
        var tuple = pattern.popBottom();
        assertEquals(new Join(pkg("C"), "id"), tuple.last());
        assertEquals("pkg.A-[child]->pkg.B", tuple.first().toString());
    }

    @Test
    public void startsWith() {
        var pattern = Pattern.from(pkg("A")).ref(pkg("B"), "b").ref(pkg("C"), "c");
        var sub = Pattern.from(pkg("A")).ref(pkg("B"), "b");
        assertTrue(pattern.startsWith(sub));
    }

    @Test
    public void startsNotWith() {
        var pattern = Pattern.from(pkg("A")).ref(pkg("B"), "b").ref(pkg("C"), "c");
        var sub = Pattern.from(pkg("A")).ref(pkg("C"), "c");
        assertFalse(pattern.startsWith(sub));
        sub = Pattern.from(pkg("B")).ref(pkg("C"), "c");
        assertFalse(pattern.startsWith(sub));
    }
}