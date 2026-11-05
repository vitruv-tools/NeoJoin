package tools.vitruv.neojoin.validation;

import org.eclipse.xtext.validation.Check;
import tools.vitruv.neojoin.Constants;
import tools.vitruv.neojoin.ast.AstPackage;
import tools.vitruv.neojoin.ast.Import;
import tools.vitruv.neojoin.ast.ViewTypeDefinition;
import tools.vitruv.neojoin.utils.AstUtils;

import java.util.stream.Collectors;

public class ImportValidator extends ComposableValidator {

    @Check
    public void checkAliasNotEcore(Import imp) {
        var alias = AstUtils.getImportAlias(imp);
        if (Constants.EcoreAlias.equals(alias)) {
            error(
                "Import alias 'ecore' is reserved for the Ecore package",
                imp,
                imp.getAlias() != null ? AstPackage.Literals.IMPORT__ALIAS : AstPackage.Literals.IMPORT__PACKAGE
            );
        }
    }

    @Check
    public void checkDuplicatedImportURIs(ViewTypeDefinition viewType) {
        var groupedURIs = viewType.getImports().stream()
            .filter(imp -> imp.getPackage() != null)
            .collect(Collectors.groupingBy(imp -> imp.getPackage().getNsURI()));
        groupedURIs.entrySet().stream()
            .filter(entry -> entry.getValue().size() > 1)
            .forEach(entry -> {
                for (var imp : entry.getValue()) {
                    error("Duplicated import URI", imp, AstPackage.Literals.IMPORT__PACKAGE);
                }
            });
    }

    @Check
    public void checkDuplicatedImportNames(ViewTypeDefinition viewType) {
        var groupedNames = viewType.getImports().stream()
            .filter(imp -> imp.getPackage() != null)
            .collect(Collectors.groupingBy(AstUtils::getImportAlias));
        groupedNames.entrySet().stream()
            .filter(entry -> entry.getValue().size() > 1)
            .forEach(entry -> {
                for (var imp : entry.getValue()) {
                    error(
                        "Duplicated import named '%s'".formatted(entry.getKey()),
                        imp,
                        imp.getAlias() != null ? AstPackage.Literals.IMPORT__ALIAS : AstPackage.Literals.IMPORT__PACKAGE
                    );
                }
            });
    }

}
