package tools.vitruv.neojoin.cli;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.xmi.impl.URIHandlerImpl;

public class RelativeURIResolver extends URIHandlerImpl {

	private final URI base;

	public RelativeURIResolver(URI base) {
		this.base = base;
	}

	@Override
	public URI deresolve(URI uri) {
		if (uri.trimFragment().equals(base)) {
			uri = URI.createURI("#" + uri.fragment());
		}
		return super.deresolve(uri);
	}

}
