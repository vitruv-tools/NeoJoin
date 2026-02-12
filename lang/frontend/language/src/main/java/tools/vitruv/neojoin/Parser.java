package tools.vitruv.neojoin;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.xtext.Constants;
import org.eclipse.xtext.diagnostics.Severity;
import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.validation.CheckMode;
import org.eclipse.xtext.validation.IResourceValidator;
import org.eclipse.xtext.validation.Issue;
import tools.vitruv.neojoin.aqr.AQR;
import tools.vitruv.neojoin.aqr.AQRBuilder;
import tools.vitruv.neojoin.ast.ViewTypeDefinition;
import tools.vitruv.neojoin.jvmmodel.ExpressionHelper;

import java.util.List;

import static tools.vitruv.neojoin.utils.Assertions.require;

/**
 * Small wrapper class for parsing a given file, validating it and building the corresponding {@link AQR}.
 *
 * @see #parse(URI)
 */
public class Parser {

    public sealed interface Result {

        List<Issue> issues();

        record Success(
            AQR aqr,
            List<Issue> issues
        ) implements Result {}

        record Failure(
            List<Issue> issues
        ) implements Result {}

    }

    private final ResourceSet resourceSet = new ResourceSetImpl();

    @Inject
    private IResourceValidator validator;

    @Inject
    @Named(Constants.FILE_EXTENSIONS)
    private String fileExtension;

    @Inject
    private ExpressionHelper expressionHelper;

    public Result parse(URI file) {
        require(
            file.fileExtension().equals(fileExtension),
            () -> "Cannot parse given file: unknown file extension (expected: %s, actual: %s)".formatted(
                fileExtension,
                file.fileExtension()
            )
        );

        // load file + parse
        var resource = resourceSet.getResource(file, true);

        // resolve lazy references (proxies) and validate parser result
        var issues = validator.validate(resource, CheckMode.ALL, CancelIndicator.NullImpl);
        var hasError = issues.stream().anyMatch(issue -> issue.getSeverity() == Severity.ERROR);
        if (hasError) {
            return new Result.Failure(issues);
        }

        if (resource.getContents().isEmpty()) {
            return new Result.Failure(List.of());
        }

        var aqr = new AQRBuilder((ViewTypeDefinition) resource.getContents().get(0), expressionHelper).build();
        return new Result.Success(aqr, issues);
    }

}
