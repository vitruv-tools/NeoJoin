package tools.vitruv.optggs.transpiler.operators.filters;

import org.junit.jupiter.api.Test;
import tools.vitruv.optggs.operators.FQN;
import tools.vitruv.optggs.operators.filters.FunctionFilter;
import tools.vitruv.optggs.transpiler.TranspilerQueryResolver;
import tools.vitruv.optggs.operators.expressions.ConstantExpression;
import tools.vitruv.optggs.operators.Query;
import tools.vitruv.optggs.operators.View;
import tools.vitruv.optggs.transpiler.operators.ResolvedQuery;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FunctionFilterTest {
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
    public void createFunctionFilter() {
        var filter = new FunctionFilter("startsWith");
        filter.setConstrainedArgument("self", pkg("A"), "name");
        filter.setConstantArgument("prefix", ConstantExpression.String("foo"));
        assertEquals("φ(pkg.A::name->startsWith(prefix: \"foo\"))", filter.toString());
    }

    @Test
    public void functionFilterToRule() {
        var filter = new FunctionFilter("startsWith");
        filter.setConstrainedArgument("self", pkg("A"), "name");
        filter.setConstantArgument("prefix", ConstantExpression.String("foo"));
        var view = new View();
        view.addQuery(Query.from(pkg("A")).create(t("A'")).build().filter(filter));
        var queries = resolve(view);
        assertEquals(1, queries.size());
        var query = queries.get(0);
        assertEquals("[src: [<++a: pkg.A;;.name==<name>>] tgt: [<++a': t.A';;>] corr: [++a<-->a'] cs: [startsWith(prefix: \"foo\",self: <name>)]]", query.toRules().toString());
    }

    @Test
    public void multiNodesFunctionFilterToRule() {
        var filter = new FunctionFilter("concatWith");
        filter.setConstrainedArgument("return", pkg("A"), "name");
        filter.setConstrainedArgument("self", pkg("B"), "prename");
        filter.setConstrainedArgument("text", pkg("B"), "surname");
        assertEquals("φ(pkg.A::name=pkg.B::prename->concatWith(text: pkg.B::surname))", filter.toString());
        var view = new View();
        view.addQuery(Query.from(pkg("A")).ref(pkg("B"), "b").create(t("A'")).build().filter(filter));
        var queries = resolve(view);
        assertEquals(1, queries.size());
        var query = queries.get(0);
        assertEquals("[src: [<++a: pkg.A;++-[b]->b;.name==<name>>, <++b: pkg.B;;.surname==<surname>,.prename==<prename>>] tgt: [<++a': t.A';;>] corr: [++a<-->a', ++b<-->a'] cs: [concatWith(self: <prename>,text: <surname>,return: <name>)]]", query.toRules().toString());
    }
}