package tools.vitruv.optggs.operators;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class FunctionInvocation {
    public interface Argument {
        String print();
    }

    public record ConstantArgument(String value) implements Argument {
        @Override
        public String print() {
            return value;
        }
    }

    public record ConstrainedArgument(FQN node, String attribute) implements Argument {
        @Override
        public String print() {
            if (node != null) {
                return node.fqn() + "::" + attribute;
            } else {
                return attribute;
            }
        }
    }

    private final String name;
    private final Map<String, Argument> args = new HashMap<>();

    public FunctionInvocation(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    public Set<String> parameters() {
        return args.keySet();
    }

    public Argument argument(String parameter) {
        return args.get(parameter);
    }

    public FunctionInvocation setConstantArgument(String parameter, String value) {
        args.put(parameter, new ConstantArgument(value));
        return this;
    }

    public FunctionInvocation setConstrainedArgument(String parameter, FQN node, String attribute) {
        args.put(parameter, new ConstrainedArgument(node, attribute));
        return this;
    }

    public String print() {
        var prefix = "";
        if (parameters().contains("return")) {
            var returnArg = argument("return");
            prefix = returnArg.print() + "=";
        }
        var argList = parameters().stream().filter(p -> !p.equals("self") && !p.equals("return")).map(p -> p + ": " + argument(p).print()).toList();
        if (parameters().contains("self")) {
            var selfArg = argument("self");
            return prefix + selfArg.print() + "->" + name + "(" + String.join(", ", argList) + ")";
        } else {
            return prefix + name + "(" + String.join(", ", argList) + ")";
        }
    }

    @Override
    public String toString() {
        return print();
    }
}
