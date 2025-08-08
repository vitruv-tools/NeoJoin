package tools.vitruv.neojoin.cli;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.impl.URIHandlerImpl;

/**
 * Ensures that local elements in a model are serialized using a fragment URI instead of the full URI.
 * <br><br>
 * <b>Example:</b>
 * <ul>
 *     <li>with RelativeURIResolver: {@code eType="#//Food"}</li>
 *     <li>without: {@code eType="ecore:EClass ../test/test.ecore#//Food"}</li>
 * </ul>
 * <br>
 * <b>Usage:</b>
 * {@code resource.save(Map.of(XMLResource.OPTION_URI_HANDLER, new RelativeURIResolver(resource.getURI())))}
 */
public class RelativeURIResolver extends URIHandlerImpl {

    private final URI base;

    public RelativeURIResolver(URI base) {
        this.base = base;
    }

    public RelativeURIResolver(Resource resource) {
        this(resource.getURI());
    }

    @Override
    public URI deresolve(URI uri) {
        if (uri.trimFragment().equals(base)) {
            uri = URI.createURI("#" + uri.fragment());
        }
        return super.deresolve(uri);
    }

}
