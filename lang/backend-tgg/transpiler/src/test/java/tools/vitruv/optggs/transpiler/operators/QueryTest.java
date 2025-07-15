package tools.vitruv.optggs.transpiler.operators;

import org.junit.jupiter.api.Test;
import tools.vitruv.optggs.operators.*;
import tools.vitruv.optggs.operators.projections.SimpleProjection;
import tools.vitruv.optggs.operators.selection.Pattern;
import tools.vitruv.optggs.operators.expressions.ConstantExpression;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QueryTest {

    private static FQN pkg(String name) {
        return new FQN("pkg", name);
    }

    private static FQN t(String name) {
        return new FQN("t", name);
    }

    @Test
    public void onlySelectionQuery() {
        var query = Query.from(pkg("A")).create(t("A'")).build();
        assertEquals("σ(pkg.A => t.A')", query.toString());
    }

    @Test
    public void singleProjection() {
        var query = Query.from(pkg("A")).create(t("A'")).build()
                .project(new SimpleProjection(Pattern.from(pkg("A")), t("A'"), "id", "name"));
        assertEquals("σ(pkg.A => t.A')π(pkg.A::id => t.A'::name)", query.toString());
    }

    @Test
    public void joinedProjection() {
        var projection = new SimpleProjection(Pattern.from(pkg("A")).ref(pkg("B"), "b"), new FQN("t", "A'"), "id", "name");
        var query = Query.from(pkg("A")).create(t("A'")).build()
                .project(projection);
        assertEquals("σ(pkg.A => t.A')π(pkg.A-[b]->pkg.B::id => t.A'::name)", query.toString());
    }

    @Test
    public void simpleProjection() {
        var query = Query.from(pkg("A")).create(t("A'")).build().project("id");
        assertEquals("σ(pkg.A => t.A')π(pkg.A::id => t.A'::id)", query.toString());
    }

    @Test
    public void simpleFilter() {
        var query = Query.from(pkg("A")).ref(pkg("B"), "b")
                .create(t("A'")).build()
                .filter(pkg("B"), "id", LogicOperator.Equals, ConstantExpression.String("foo"));
        assertEquals("σ(pkg.A-[b]->pkg.B => t.A')φ(pkg.B::id==\"foo\")", query.toString());
    }

    @Test
    public void containment() {
        var containment = new Containment(Pattern.from(pkg("A")).ref(pkg("B"), "b"), t("A'"), "children");
        var query = Query.from(pkg("A")).create(t("A'")).build().contains(containment);
        assertEquals("σ(pkg.A => t.A')κ(pkg.A-[b]->pkg.B => t.A'-[children]->?)", query.toString());
    }

    @Test
    public void containmentAlt() {
        var query = Query.from(pkg("A")).create(t("A'")).build()
                .contains(Pattern.from(pkg("A")).ref(pkg("B"), "b"), "children");
        assertEquals("σ(pkg.A => t.A')κ(pkg.A-[b]->pkg.B => t.A'-[children]->?)", query.toString());
    }

    @Test
    public void containmentAlt2() {
        var query = Query.from(pkg("A")).create(t("A'")).build()
                .contains(pkg("B"), "b", "children");
        assertEquals("σ(pkg.A => t.A')κ(pkg.A-[b]->pkg.B => t.A'-[children]->?)", query.toString());
    }

    @Test
    public void containmentAlt3() {
        var query = Query.from(pkg("A")).create(t("A'")).build()
                .contains(pkg("B"), "b");
        assertEquals("σ(pkg.A => t.A')κ(pkg.A-[b]->pkg.B => t.A'-[b]->?)", query.toString());
    }

    @Test
    public void link() {
        var link = new Link(Pattern.from(pkg("A")).ref(pkg("B"), "b"), t("A'"), "children");
        var query = Query.from(pkg("A")).create(t("A'")).build().references(link);
        assertEquals("σ(pkg.A => t.A')λ(pkg.A-[b]->pkg.B => t.A'-[children]->?)", query.toString());
    }

    @Test
    public void linkAlt() {
        var query = Query.from(pkg("A")).create(t("A'")).build()
                .references(Pattern.from(pkg("A")).ref(pkg("B"), "b"), "children");
        assertEquals("σ(pkg.A => t.A')λ(pkg.A-[b]->pkg.B => t.A'-[children]->?)", query.toString());
    }

    @Test
    public void linkAlt2() {
        var query = Query.from(pkg("A")).create(t("A'")).build()
                .references(pkg("B"), "b", "children");
        assertEquals("σ(pkg.A => t.A')λ(pkg.A-[b]->pkg.B => t.A'-[children]->?)", query.toString());
    }

    @Test
    public void linkAlt3() {
        var query = Query.from(pkg("A")).create(t("A'")).build()
                .references(pkg("B"), "b");
        assertEquals("σ(pkg.A => t.A')λ(pkg.A-[b]->pkg.B => t.A'-[b]->?)", query.toString());
    }
}