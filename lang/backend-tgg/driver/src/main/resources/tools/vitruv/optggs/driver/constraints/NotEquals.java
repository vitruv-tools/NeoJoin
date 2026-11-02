import org.emoflon.neo.engine.modules.attributeConstraints.NeoAttributeConstraint;

import java.util.Objects;

public class NotEquals extends NeoAttributeConstraint {

    @Override
    public void solve() {
        if (variables.size() != 2) {
            throw new RuntimeException("notEquals requires two arguments");
        }

        var self = variables.get(0);
        var other = variables.get(1);

        var bindingStates = getBindingStates(self, other);

        if (bindingStates.equals("BB")) {
            setSatisfied(!Objects.equals(self.getValue(), other.getValue()));
        } else {
            throw new UnsupportedOperationException("Cannot infer self and other for notEquals");
        }
    }
}
