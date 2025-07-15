package tools.vitruv.optggs.transpiler.operators;

import org.junit.jupiter.api.Test;
import tools.vitruv.optggs.operators.*;
import tools.vitruv.optggs.transpiler.TranspilerQueryResolver;
import tools.vitruv.optggs.operators.expressions.ConstantExpression;
import tools.vitruv.optggs.operators.selection.Pattern;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LinkTest {
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
    public void complextTargetLinkToRule() {
        var link = new Link(Pattern.from(pkg("A")).ref(pkg("C"), "c"), Pattern.from(t("A'")).ref(t("B'"), "b"), "c");
        assertEquals("λ(pkg.A-[c]->pkg.C => t.A'-[b]->t.B'-[c]->?)", link.toString());
        var view = new View();
        view.addQuery(Query.from(pkg("A")).create(t("A'")).build().references(link));
        view.addQuery(Query.from(pkg("C")).create(t("C'")).build());
        assertEquals("[src: [<++a: pkg.A;;>] tgt: [<++a': t.A';;>] corr: [++a<-->a'], src: [<a: pkg.A;++-[c]->c;>, <c: pkg.C;;>] tgt: [<a': t.A';++-[b]->b';>, <++b': t.B';++-[c]->c';>, <c': t.C';;>] corr: [a<-->a', c<-->c']]", resolve(view).get(0).toRules().toString());
    }

    @Test
    public void filteredLink() {
        var link = new Link(Pattern.from(pkg("A")).ref(pkg("C"), "c"), Pattern.from(t("A'")).ref(t("B'"), "b"), "c");
        link = link.filter(pkg("C"), "id", LogicOperator.Equals, ConstantExpression.String("foo"));
        assertEquals("λ(pkg.A-[c]->pkg.C => t.A'-[b]->t.B'-[c]->? | φ(pkg.C::id==\"foo\"))", link.toString());
        var view = new View();
        view.addQuery(Query.from(pkg("A")).create(t("A'")).build().references(link));
        view.addQuery(Query.from(pkg("C")).create(t("C'")).build());
        assertEquals("[src: [<++a: pkg.A;;>] tgt: [<++a': t.A';;>] corr: [++a<-->a'], src: [<a: pkg.A;++-[c]->c;>, <c: pkg.C;;.id==\"foo\">] tgt: [<a': t.A';++-[b]->b';>, <++b': t.B';++-[c]->c';>, <c': t.C';;>] corr: [a<-->a', c<-->c']]", resolve(view).get(0).toRules().toString());
    }

    @Test
    public void simpleFilteredLink() {
        var link = new Link(Pattern.from(pkg("A")).ref(pkg("C"), "c"), Pattern.from(t("A'")).ref(t("B'"), "b"), "c");
        link = link.filter("id", ConstantExpression.String("foo"));
        assertEquals("λ(pkg.A-[c]->pkg.C => t.A'-[b]->t.B'-[c]->? | φ(pkg.C::id==\"foo\"))", link.toString());
        var view = new View();
        view.addQuery(Query.from(pkg("A")).create(t("A'")).build().references(link));
        view.addQuery(Query.from(pkg("C")).create(t("C'")).build());
        assertEquals("[src: [<++a: pkg.A;;>] tgt: [<++a': t.A';;>] corr: [++a<-->a'], src: [<a: pkg.A;++-[c]->c;>, <c: pkg.C;;.id==\"foo\">] tgt: [<a': t.A';++-[b]->b';>, <++b': t.B';++-[c]->c';>, <c': t.C';;>] corr: [a<-->a', c<-->c']]", resolve(view).get(0).toRules().toString());
    }

}