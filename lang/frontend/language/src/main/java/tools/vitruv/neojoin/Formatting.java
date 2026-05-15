package tools.vitruv.neojoin;

public class Formatting {

    public static String formatRootReferenceName(String name) {
        return new StringBuilder(name)
            .replace(0, 1, "" + Character.toLowerCase(name.charAt(0)))
            .append("s")
            .toString();
    }
}
