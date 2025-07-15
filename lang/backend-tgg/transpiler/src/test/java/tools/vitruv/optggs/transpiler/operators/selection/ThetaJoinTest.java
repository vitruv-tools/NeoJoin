package tools.vitruv.optggs.transpiler.operators.selection;

import org.junit.jupiter.api.Test;
import tools.vitruv.optggs.operators.FQN;
import tools.vitruv.optggs.operators.FunctionInvocation;
import tools.vitruv.optggs.operators.selection.Pattern;
import tools.vitruv.optggs.transpiler.TranspilerQueryResolver;
import tools.vitruv.optggs.transpiler.operators.patterns.ResolvedPattern;
import tools.vitruv.optggs.transpiler.tgg.TripleRule;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ThetaJoinTest {

    private static FQN pkg(String name) {
        return new FQN("pkg", name);
    }

    private ResolvedPattern resolve(Pattern pattern) {
        return new TranspilerQueryResolver().resolvePattern(pattern);
    }

    @Test
    public void thetaJoinToPattern() {
        var function = new FunctionInvocation("concatWith")
                .setConstrainedArgument("return", pkg("A"), "name")
                .setConstrainedArgument("self", pkg("B"), "prename")
                .setConstrainedArgument("text", pkg("B"), "surname");
        var pattern = Pattern.from(pkg("A")).join(pkg("B"), function);
        assertEquals("pkg.A ⨝(pkg.A::name=pkg.B::prename->concatWith(text: pkg.B::surname)) pkg.B", pattern.toString());
        var rule = new TripleRule();
        resolve(pattern).extendSlice(rule.allSourcesAsSlice());
        assertEquals("src: [<a: pkg.A;;.name==<name>>, <b: pkg.B;;.surname==<surname>,.prename==<prename>>] tgt: [] corr: [] cs: [concatWith(self: <prename>,text: <surname>,return: <name>)]", rule.toString());
    }

    @Test
    public void thetaAndNaturalJoinToPattern() {
        var function = new FunctionInvocation("concatWith")
                .setConstrainedArgument("return", pkg("A"), "name")
                .setConstrainedArgument("self", pkg("B"), "prename")
                .setConstrainedArgument("text", pkg("B"), "surname");
        var pattern = Pattern.from(pkg("A")).join(pkg("B"), function).join(pkg("C"), "surname");
        assertEquals("pkg.A ⨝(pkg.A::name=pkg.B::prename->concatWith(text: pkg.B::surname)) pkg.B ⨝(surname==surname) pkg.C", pattern.toString());
        var rule = new TripleRule();
        resolve(pattern).extendSlice(rule.allSourcesAsSlice());
        assertEquals("src: [<a: pkg.A;;.name==<name>>, <b: pkg.B;;.surname==<surname>,.prename==<prename>>, <c: pkg.C;;.surname==<surname>>] tgt: [] corr: [] cs: [concatWith(self: <prename>,text: <surname>,return: <name>)]", rule.toString());
    }
}