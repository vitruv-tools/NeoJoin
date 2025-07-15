package tools.vitruv.optggs.transpiler.operators.projections;

import org.junit.jupiter.api.Test;
import tools.vitruv.optggs.operators.FQN;
import tools.vitruv.optggs.operators.FunctionInvocation;
import tools.vitruv.optggs.transpiler.TranspilerQueryResolver;
import tools.vitruv.optggs.operators.Query;
import tools.vitruv.optggs.operators.View;
import tools.vitruv.optggs.transpiler.operators.ResolvedQuery;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DerivedProjectionTest {
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
    public void deriveProjectionToRule() {
        var function = new FunctionInvocation("capitalize").setConstrainedArgument("self", pkg("A"), "name");
        var query = Query.from(pkg("A")).create(t("A'")).build().project(function, "name");
        assertEquals("σ(pkg.A => t.A')π(t.A'::name=pkg.A::name->capitalize())", query.toString());
        var view = new View();
        view.addQuery(query);
        assertEquals("[src: [<++a: pkg.A;;.name==<name>>] tgt: [<++a': t.A';;.name==<name1>>] corr: [++a<-->a'] cs: [capitalize(self: <name>,return: <name1>)]]", resolve(view).get(0).toRules().toString());
    }

    @Test
    public void deriveComplexProjectionToRule() {
        var function = new FunctionInvocation("concatWith")
                .setConstrainedArgument("self", pkg("A"), "name")
                .setConstrainedArgument("text", pkg("B"), "index");
        var query = Query.from(pkg("A")).ref(pkg("B"), "b").create(t("A'")).build().project(function, "name");
        assertEquals("σ(pkg.A-[b]->pkg.B => t.A')π(t.A'::name=pkg.A::name->concatWith(text: pkg.B::index))", query.toString());
        var view = new View();
        view.addQuery(query);
        assertEquals("[src: [<++a: pkg.A;++-[b]->b;.name==<name>>, <++b: pkg.B;;.index==<index>>] tgt: [<++a': t.A';;.name==<name1>>] corr: [++a<-->a', ++b<-->a'] cs: [concatWith(self: <name>,text: <index>,return: <name1>)]]", resolve(view).get(0).toRules().toString());

    }
}