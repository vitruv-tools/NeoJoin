package tools.vitruv.neojoin.aqr;

// Here we store the URI of the reference metamodel (more specifically: the Ecore package)
//-> now uri String intead of package
public class AQRConforms {
    private final String uri;

    public AQRConforms(String uri) {
        this.uri = uri;
    }

    public String uri() {
        return uri;
    }
}
