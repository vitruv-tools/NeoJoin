package tools.vitruv.optggs.transpiler.operators;

import org.junit.jupiter.api.Test;
import tools.vitruv.optggs.operators.Containment;
import tools.vitruv.optggs.operators.FQN;
import tools.vitruv.optggs.operators.Query;
import tools.vitruv.optggs.operators.View;
import tools.vitruv.optggs.transpiler.TranspilerQueryResolver;
import tools.vitruv.optggs.operators.expressions.ConstantExpression;
import tools.vitruv.optggs.operators.selection.Pattern;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ContainmentTest {

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
    public void filteredContainment() {
        var containment = new Containment(Pattern.from(pkg("A")).ref(pkg("B"), "b"), t("A'"), "b")
                .filter("id", ConstantExpression.String("foo"));
        assertEquals("κ(pkg.A-[b]->pkg.B => t.A'-[b]->? | φ(pkg.B::id==\"foo\"))", containment.toString());
        var view = new View();
        view.addQuery(Query.from(pkg("A")).create(t("A'")).build().contains(containment));
        view.addQuery(Query.from(pkg("B")).create(t("B'")).build());
        assertEquals("[src: [<++b: pkg.B;;.id==\"foo\">, <a: pkg.A;++-[b]->b;>] tgt: [<++b': t.B';;>, <a': t.A';++-[b]->b';>] corr: [++b<-->b', a<-->a']]", resolve(view).get(1).toRules().toString());
    }
}