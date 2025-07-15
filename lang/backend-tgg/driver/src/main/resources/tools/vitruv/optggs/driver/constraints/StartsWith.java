import org.emoflon.neo.engine.modules.attributeConstraints.NeoAttributeConstraint;

public class StartsWith extends NeoAttributeConstraint {

    @Override
    public void solve() {
        if (variables.size() != 2) {
            throw new RuntimeException("startsWith requires two arguments");
        }

        var self = variables.get(0);
        var prefix = variables.get(1);

        var bindingStates = getBindingStates(self, prefix);

        if (bindingStates.equals("BB")) {
            var textLength = self.getValue().toString().length();
            var prefixLength = prefix.getValue().toString().length();
            var text = self.getValue().toString();
            var prefixText = prefix.getValue().toString();

            setSatisfied(textLength >= prefixLength && text.startsWith(prefixText));
        } else if (bindingStates.equals("FB")) {
            self.bindToValue(prefix.getValue().toString());
            setSatisfied(true);
        } else if(bindingStates.equals("BF")) {
            prefix.bindToValue(self.getValue().toString());
            setSatisfied(true);
        }else {
            throw new UnsupportedOperationException("Cannot infer self and prefix for startsWith");
        }
    }
}
