package tools.vitruv.optggs.transpiler.tgg;

import tools.vitruv.optggs.operators.FQN;

import java.util.ArrayList;

public class NameRepository {
    private final ArrayList<String> names = new ArrayList<>();

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

    public String get(FQN preference) {
        return get(preference.localName());
    }

    public String getLower(String preference) {
        return get(preference.toLowerCase());
    }

    public String getLower(FQN preference) {
        return get(preference.localName().toLowerCase());
    }
}
