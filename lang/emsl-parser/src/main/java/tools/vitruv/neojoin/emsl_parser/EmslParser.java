package tools.vitruv.neojoin.emsl_parser;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.xtext.resource.IResourceServiceProvider;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.validation.CheckMode;
import org.eclipse.xtext.validation.Issue;
import org.emoflon.neo.emsl.EMSLStandaloneSetup;

import java.util.List;

public class EmslParser {
    static {
        new EMSLStandaloneSetup().createInjectorAndDoEMFRegistration();
    }

    public static List<EObject> parse(String path) {
        // load
        var resourceServiceProvider =
                IResourceServiceProvider.Registry.INSTANCE.getResourceServiceProvider(
                        URI.createFileURI(path));
        if (resourceServiceProvider == null) {
            throw new IllegalStateException("ResourceServiceProvider not found");
        }

        var resourceSet = resourceServiceProvider.get(ResourceSet.class);
        var resource = resourceSet.getResource(URI.createFileURI(path), true);

        // validate
        var validator =
                ((XtextResource) resource).getResourceServiceProvider().getResourceValidator();
        var issues = validator.validate(resource, CheckMode.ALL, CancelIndicator.NullImpl);
        if (!issues.isEmpty()) {
            throw new RuntimeException(generateIssuesMessage(issues));
        }

        return resource.getContents();
    }

    private static String generateIssuesMessage(List<Issue> issues) {
        return String.join(System.lineSeparator(), issues.stream().map(Issue::getMessage).toList());
    }
}
