package tools.vitruv.optggs.transpiler.graph;

import tools.vitruv.optggs.operators.FQN;

import java.util.ArrayList;

public class NameRepository {
    private final ArrayList<String> names = new ArrayList<>();

    /** Returns a variable name for the given String that is not already taken */
    public String get(String preference) {
        String name = preference;
        int suffix = 1;
        while (names.contains(name)) {
            name = preference + suffix;
            suffix++;
        }
        names.add(name);
        return name;
    }

    /** Returns a variable name for the given FQN that is not already taken */
    public String get(FQN preference) {
        return get(preference.localName());
    }

    /** Returns a lowercase variable name for the given String that is not already taken */
    public String getLower(String preference) {
        return get(preference.toLowerCase());
    }

    /** Returns a lowercase variable name for the given FQN that is not already taken */
    public String getLower(FQN preference) {
        return get(preference.localName().toLowerCase());
    }

    public NameRepository deepCopy() {
        final NameRepository clone = new NameRepository();
        clone.names.addAll(names);
        return clone;
    }
}
