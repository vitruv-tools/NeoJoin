package tools.vitruv.optggs.transpiler.operators;

import org.junit.jupiter.api.Test;
import tools.vitruv.optggs.operators.FQN;
import tools.vitruv.optggs.operators.Mapping;
import tools.vitruv.optggs.operators.Query;
import tools.vitruv.optggs.operators.View;
import tools.vitruv.optggs.transpiler.TranspilerQueryResolver;
import tools.vitruv.optggs.operators.selection.Pattern;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ViewTest {

    private static FQN pkg(String name) {
        return new FQN("pkg", name);
    }

    private static FQN t(String name) {
        return new FQN("t", name);
    }

    private List<ResolvedQuery> resolve(View view) {
        return new TranspilerQueryResolver().resolveView(view).queries();
    }

    private View simpleView() {
        View view = new View();
        view.addQuery(Query.from(pkg("A")).create(t("A'")).build().contains(pkg("B"), "b").contains(pkg("C"), "c"));
        view.addQuery(Query.from(pkg("B")).create(t("B'")).build().project("id", "id"));
        view.addQuery(Query.from(pkg("C")).create(t("C'")).build().references(pkg("B"), "b"));
        return view;
    }

    private View complexContainment() {
        View view = new View();
        view.addQuery(Query.from(pkg("A")).create(t("A'")).build()
                .contains(Pattern.from(pkg("A")).ref(pkg("B"), "b").ref(pkg("C"), "c"), "child"));
        view.addQuery(Query.from(pkg("C")).create(t("C'")).build());

        return view;
    }

    @Test
    public void normalizeSimpleContainment() {
        var view = simpleView();
        var resolver = new TranspilerQueryResolver();
        var containers = resolver.findContainers(view);
        assertEquals(2, containers.size());
        assertEquals("Κ(pkg.A-[b]->pkg.B => t.A'-[b]->t.B')", containers.get(new Mapping(pkg("B"), t("B'"))).toString());
        assertEquals("Κ(pkg.A-[c]->pkg.C => t.A'-[c]->t.C')", containers.get(new Mapping(pkg("C"), t("C'"))).toString());
    }

    @Test
    public void normalizeComplexContainment() {
        var view = complexContainment();
        var resolver = new TranspilerQueryResolver();
        var containers = resolver.findContainers(view);
        assertEquals(1, containers.size());
        assertEquals("Κ(pkg.A-[b]->pkg.B-[c]->pkg.C => t.A'-[child]->t.C')", containers.get(new Mapping(pkg("C"), t("C'"))).toString());
    }

    @Test
    public void resolveSimpleContainment() {
        var view = simpleView();
        var queries = resolve(view).stream().map(Object::toString).toList();
        assertEquals(3, queries.size());
        assertTrue(queries.contains("Σ(pkg.A => t.A')"));
        assertTrue(queries.contains("Σ(pkg.B => t.B')Π(pkg.B::id => t.B'::id)Κ(pkg.A-[b]->pkg.B => t.A'-[b]->t.B')"));
        assertTrue(queries.contains("Σ(pkg.C => t.C')Κ(pkg.A-[c]->pkg.C => t.A'-[c]->t.C')Λ(pkg.C-[b]->pkg.B => t.C'-[b]->t.B')"));
    }

    @Test
    public void resolveComplexContainment() {
        var view = complexContainment();
        var queries = resolve(view).stream().map(Objects::toString).toList();
        assertEquals(2, queries.size());
        assertTrue(queries.contains("Σ(pkg.A => t.A')"));
        assertTrue(queries.contains("Σ(pkg.C => t.C')Κ(pkg.A-[b]->pkg.B-[c]->pkg.C => t.A'-[child]->t.C')"));
    }
}