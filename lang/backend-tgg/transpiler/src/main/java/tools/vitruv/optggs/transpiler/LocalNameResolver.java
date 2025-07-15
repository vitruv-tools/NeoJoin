package tools.vitruv.optggs.transpiler;

import tools.vitruv.optggs.operators.FQN;

public class LocalNameResolver implements NameResolver {
    @Override
    public String resolveName(FQN fqn) {
        return fqn.localName();
    }
}
