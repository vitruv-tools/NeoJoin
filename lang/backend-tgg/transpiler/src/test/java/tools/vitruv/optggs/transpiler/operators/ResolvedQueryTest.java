package tools.vitruv.optggs.transpiler.operators;

import org.junit.jupiter.api.Test;
import tools.vitruv.optggs.operators.FQN;
import tools.vitruv.optggs.operators.Query;
import tools.vitruv.optggs.operators.View;
import tools.vitruv.optggs.transpiler.TranspilerQueryResolver;
import tools.vitruv.optggs.operators.selection.Pattern;
import tools.vitruv.optggs.operators.selection.Union;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResolvedQueryTest {
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
    public void simpleQueryToRules() {
        var view = new View();
        view.addQuery(Query.from(pkg("A")).create(t("A'")).build().contains(pkg("B"), "b"));
        view.addQuery(Query.from(pkg("B")).create(t("B'")).build());
        var queries = resolve(view);
        var queryA = queries.get(0);
        var queryB = queries.get(1);

        assertEquals("[src: [<++a: pkg.A;;>] tgt: [<++a': t.A';;>] corr: [++a<-->a']]", queryA.toRules().toString());
        assertEquals("[src: [<++b: pkg.B;;>, <a: pkg.A;++-[b]->b;>] tgt: [<++b': t.B';;>, <a': t.A';++-[b]->b';>] corr: [++b<-->b', a<-->a']]", queryB.toRules().toString());
    }

    @Test
    public void linkedQueryToRules() {
        var view = new View();
        view.addQuery(Query.from(pkg("A")).create(t("A'")).build().references(pkg("B"), "b"));
        view.addQuery(Query.from(pkg("B")).create(t("B'")).build());
        var queries = resolve(view);
        var queryA = queries.get(0);
        var queryB = queries.get(1);

        assertEquals("[src: [<++a: pkg.A;;>] tgt: [<++a': t.A';;>] corr: [++a<-->a'], src: [<a: pkg.A;++-[b]->b;>, <b: pkg.B;;>] tgt: [<a': t.A';++-[b]->b';>, <b': t.B';;>] corr: [a<-->a', b<-->b']]", queryA.toRules().toString());
        assertEquals("[src: [<++b: pkg.B;;>] tgt: [<++b': t.B';;>] corr: [++b<-->b']]", queryB.toRules().toString());
    }

    @Test
    public void resolveContainedUnion() {
        var view = new View();
        var union = new Union(Pattern.from(pkg("A")).ref(pkg("B"), "children")).add(Pattern.from(pkg("A")).ref(pkg("C"), "children"));
        view.addQuery(Query.from(pkg("A")).create(t("A'")).build().contains(union, "children"));
        view.addQuery(Query.from(pkg("B")).create(t("B'")).build());
        view.addQuery(Query.from(pkg("C")).create(t("C'")).build());

        var queries = resolve(view);
        var queryA = queries.get(0);
        var queryB = queries.get(1);
        var queryC = queries.get(2);

        assertEquals("[src: [<++a: pkg.A;;>] tgt: [<++a': t.A';;>] corr: [++a<-->a']]", queryA.toRules().toString());
        assertEquals("[src: [<++b: pkg.B;;>, <a: pkg.A;++-[children]->b;>] tgt: [<++b': t.B';;>, <a': t.A';++-[children]->b';>] corr: [++b<-->b', a<-->a']]", queryB.toRules().toString());
        assertEquals("[src: [<++c: pkg.C;;>, <a: pkg.A;++-[children]->c;>] tgt: [<++c': t.C';;>, <a': t.A';++-[children]->c';>] corr: [++c<-->c', a<-->a']]", queryC.toRules().toString());
    }

    @Test
    public void resolveLinkedUnion() {
        var view = new View();
        var sources = new Union(Pattern.from(pkg("A")).ref(pkg("B"), "children")).add(Pattern.from(pkg("A")).ref(pkg("C"), "children"));
        view.addQuery(Query.from(pkg("A")).create(t("A'")).build().references(sources, "children"));
        view.addQuery(Query.from(pkg("B")).create(t("B'")).build());
        view.addQuery(Query.from(pkg("C")).create(t("C'")).build());
        var queries = resolve(view);
        var queryA = queries.get(0);
        var queryB = queries.get(1);
        var queryC = queries.get(2);
        assertEquals("[src: [<++a: pkg.A;;>] tgt: [<++a': t.A';;>] corr: [++a<-->a'], src: [<a: pkg.A;++-[children]->b;>, <b: pkg.B;;>] tgt: [<a': t.A';++-[children]->b';>, <b': t.B';;>] corr: [a<-->a', b<-->b'], src: [<a: pkg.A;++-[children]->c;>, <c: pkg.C;;>] tgt: [<a': t.A';++-[children]->c';>, <c': t.C';;>] corr: [a<-->a', c<-->c']]", queryA.toRules().toString());
        assertEquals("[src: [<++b: pkg.B;;>] tgt: [<++b': t.B';;>] corr: [++b<-->b']]", queryB.toRules().toString());
        assertEquals("[src: [<++c: pkg.C;;>] tgt: [<++c': t.C';;>] corr: [++c<-->c']]", queryC.toRules().toString());
    }
}