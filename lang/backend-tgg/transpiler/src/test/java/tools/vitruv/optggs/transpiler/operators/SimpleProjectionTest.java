package tools.vitruv.optggs.transpiler.operators;

import org.junit.jupiter.api.Test;
import tools.vitruv.optggs.operators.Containment;
import tools.vitruv.optggs.operators.FQN;
import tools.vitruv.optggs.operators.Query;
import tools.vitruv.optggs.operators.View;
import tools.vitruv.optggs.transpiler.TranspilerQueryResolver;
import tools.vitruv.optggs.operators.projections.SimpleProjection;
import tools.vitruv.optggs.operators.selection.Pattern;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SimpleProjectionTest {
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
    public void simpleProjectionToRule() {
        var view = new View();
        view.addQuery(Query.from(pkg("A")).create(t("A'")).build().project("id"));
        var queries = resolve(view);
        assertEquals(1, queries.size());
        var query = queries.get(0);
        assertEquals("[src: [<++a: pkg.A;;.id==<id>>] tgt: [<++a': t.A';;.id==<id>>] corr: [++a<-->a']]", query.toRules().toString());
    }

    @Test
    public void linkedProjectionToRule() {
        var view = new View();
        view.addQuery(
                Query.from(pkg("A")).create(t("A'")).build()
                        .project(new SimpleProjection(Pattern.from(pkg("A")).ref(pkg("B"), "b"), t("A'"), "id", "id"))
        );
        var queries = resolve(view);
        assertEquals(1, queries.size());
        var query = queries.get(0);
        assertEquals("[src: [<++a: pkg.A;-[b]->b;>, <b: pkg.B;;.id==<id>>] tgt: [<++a': t.A';;.id==<id>>] corr: [++a<-->a']]", query.toRules().toString());
    }

    @Test
    public void multiTargetProjectionToRule() {
        var view = new View();
        view.addQuery(
                Query.from(pkg("A")).create(t("A'")).ref(t("B'"), "b").build()
                        .project(new SimpleProjection(Pattern.from(pkg("A")), t("B'"), "id", "id"))
        );
        var queries = resolve(view);
        assertEquals(1, queries.size());
        var query = queries.get(0);
        assertEquals("[src: [<++a: pkg.A;;.id==<id>>] tgt: [<++a': t.A';++-[b]->b';>, <++b': t.B';;.id==<id>>] corr: [++a<-->a', ++a<-->b']]", query.toRules().toString());
    }

    @Test
    public void checkDirectContainmentInQuery() {
        var projection = new SimpleProjection(Pattern.from(pkg("A")).ref(pkg("B"), "b"), t("A'"), "id", "id");
        var containment = new Containment(Pattern.from(pkg("A")).ref(pkg("B"), "b"), t("A'"), "b");
        assertTrue(projection.isContained(List.of(containment), projection.source()));
    }

    @Test
    public void checkDirectPartialContainmentInQuery() {
        var projection = new SimpleProjection(Pattern.from(pkg("A")).ref(pkg("B"), "b"), t("A'"), "id", "id");
        var containment = new Containment(Pattern.from(pkg("A")).ref(pkg("B"), "b").ref(pkg("C"), "c"), t("A'"), "c");
        assertTrue(projection.isContained(List.of(containment), projection.source()));
    }

    @Test
    public void checkNoDirectContainmentInQuery() {
        var projection = new SimpleProjection(Pattern.from(pkg("A")).ref(pkg("B"), "b"), t("A'"), "id", "id");
        var containment = new Containment(Pattern.from(pkg("A")).ref(pkg("C"), "c"), t("A'"), "c");
        assertFalse(projection.isContained(List.of(containment), projection.source()));
    }

    @Test
    public void checkNoSelectionEver() {
        var view = new View();
        var projection = new SimpleProjection(Pattern.from(pkg("A")).ref(pkg("B"), "b"), t("A'"), "id", "id");
        view.addQuery(Query.from(pkg("A")).create(t("A'")).build().project(projection).contains(pkg("C"), "c"));
        view.addQuery(Query.from(pkg("C")).create(t("C'")).build().project(projection));
        assertTrue(view.mappings().stream().noneMatch((mapping) -> mapping.source().equals(pkg("B"))));
        assertFalse(view.mappings().stream().noneMatch((mapping) -> mapping.source().equals(pkg("C"))));
    }

}