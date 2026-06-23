package tools.vitruv.neojoin;

public class Formatting {
    
    private Formatting() {}

    /**
    * Formats a root reference name into a canonical representation.
    *
    * @param name the root reference name to format; must not be {@code null} nor empty
    * @return the formatted root reference name
    *
    */
    public static String formatRootReferenceName(String name) {
        return new StringBuilder(name)
            .replace(0, 1, "" + Character.toLowerCase(name.charAt(0)))
            .append("s")
            .toString();
    }
}
