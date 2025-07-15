import org.emoflon.neo.engine.modules.attributeConstraints.NeoAttributeConstraint;

public class Concat extends NeoAttributeConstraint {

    @Override
    public void solve() {
        if (variables.size() != 3) {
            throw new RuntimeException("concat requires three arguments");
        }

        var self = variables.get(0);
        var text = variables.get(1);
        var result = variables.get(2);

        var bindingStates = getBindingStates(self, text, result);

        if (bindingStates.equals("BBB")) {
            var prefix = self.getValue().toString();
            var suffix = text.getValue().toString();
            var full = result.getValue().toString();

            setSatisfied(prefix.concat(suffix).equals(full));
        } else if (bindingStates.equals("BBF")) {
            var prefix = self.getValue().toString();
            var suffix = text.getValue().toString();

            result.bindToValue(prefix.concat(suffix));
            setSatisfied(true);
        } else if (bindingStates.equals("FBB")) {
            var suffix = text.getValue().toString();
            var full = result.getValue().toString();
            var isSuffix = suffix.length() <= full.length() && full.endsWith(suffix);

            setSatisfied(isSuffix);
            if (isSuffix) {
                var prefix = full.substring(0,  full.length() - suffix.length());
                self.bindToValue(prefix);
            }
        } else if (bindingStates.equals("BFB")) {
            var prefix = self.getValue().toString();
            var full = result.getValue().toString();
            var isPrefix = prefix.length() <= full.length() && full.startsWith(prefix);

            setSatisfied(isPrefix);
            if (isPrefix) {
                var suffix = full.substring(prefix.length());
                text.bindToValue(suffix);
            }
        } else if (bindingStates.equals("FFB")) {
            var full = result.getValue().toString();
            self.bindToValue(full);
            text.bindToValue("");
            setSatisfied(true);
        } else if (bindingStates.equals("BFF")) {
            var prefix = self.getValue().toString();
            text.bindToValue("");
            result.bindToValue(prefix);
            setSatisfied(true);
        } else if (bindingStates.equals("FBF")) {
            var suffix = text.getValue().toString();
            self.bindToValue("");
            result.bindToValue(suffix);
            setSatisfied(true);
        } else if (bindingStates.equals("FFF")) {
            self.bindToValue("");
            text.bindToValue("");
            result.bindToValue("");
            setSatisfied(true);
        } else {
            throw new UnsupportedOperationException("Cannot infer self and prefix for startsWith");
        }
    }
}
