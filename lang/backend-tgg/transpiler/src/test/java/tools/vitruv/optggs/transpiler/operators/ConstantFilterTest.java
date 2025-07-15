package tools.vitruv.optggs.transpiler.operators;

import org.junit.jupiter.api.Test;
import tools.vitruv.optggs.operators.FQN;
import tools.vitruv.optggs.operators.LogicOperator;
import tools.vitruv.optggs.operators.Query;
import tools.vitruv.optggs.operators.View;
import tools.vitruv.optggs.transpiler.TranspilerQueryResolver;
import tools.vitruv.optggs.operators.expressions.ConstantExpression;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConstantFilterTest {

    private static FQN pkg(String name) {
        return new FQN("pkg", name);
    }

    private static FQN t(String name) {
        return new FQN("t", name);
    }

    private List<ResolvedQuery> resolve(View view) {
        return new TranspilerQueryResolver().resolveView(view).queries();
    }

    @Test
    public void filterToRule() {
        var view = new View();
        view.addQuery(Query.from(pkg("A")).create(t("A'")).build().filter(pkg("A"), "id", LogicOperator.Equals, ConstantExpression.String("foo")));
        var queries = resolve(view);
        assertEquals(1, queries.size());
        var query = queries.get(0);
        assertEquals("[src: [<++a: pkg.A;;.id==\"foo\">] tgt: [<++a': t.A';;>] corr: [++a<-->a']]", query.toRules().toString());
    }

    @Test
    public void multiNodeFilterToRule() {
        var view = new View();
        view.addQuery(Query.from(pkg("A")).ref(pkg("B"), "b").create(t("A'")).build().filter(pkg("A"), "id", LogicOperator.Equals, ConstantExpression.String("foo")));
        var queries = resolve(view);
        assertEquals(1, queries.size());
        var query = queries.get(0);
        assertEquals("[src: [<++a: pkg.A;++-[b]->b;.id==\"foo\">, <++b: pkg.B;;>] tgt: [<++a': t.A';;>] corr: [++a<-->a', ++b<-->a']]", query.toRules().toString());
    }

    @Test
    public void multiNodeFilterToRuleAlt() {
        var view = new View();
        view.addQuery(Query.from(pkg("A")).ref(pkg("B"), "b").create(t("A'")).build().filter(pkg("B"), "id", LogicOperator.Equals, ConstantExpression.String("foo")));
        var queries = resolve(view);
        assertEquals(1, queries.size());
        var query = queries.get(0);
        assertEquals("[src: [<++a: pkg.A;++-[b]->b;>, <++b: pkg.B;;.id==\"foo\">] tgt: [<++a': t.A';;>] corr: [++a<-->a', ++b<-->a']]", query.toRules().toString());
    }
}