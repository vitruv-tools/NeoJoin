package tools.vitruv.neojoin.ide.custom;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.xbase.linking.BrokenConstructorCallAwareEObjectAtOffsetHelper;
import org.jspecify.annotations.Nullable;
import tools.vitruv.neojoin.ast.AstPackage;

import java.util.Set;

/**
 * Recognizes the aliases of sources ({@code from Restaurant r}) and the names of query parameters
 * ({@code param minEmployees: EInt}) as the names of the elements they declare.
 * <p>
 * Xtext only recognizes an attribute called {@code name}, but these declarations call it {@code alias}. Without this,
 * a definition request on a declaration finds nothing, so a references request on it (which includes the
 * declaration) lists only the uses.
 */
public class AliasAwareEObjectAtOffsetHelper extends BrokenConstructorCallAwareEObjectAtOffsetHelper {

    private static final Set<EAttribute> DECLARED_NAMES = Set.of(
        AstPackage.Literals.FROM__ALIAS,
        AstPackage.Literals.PARAMETER__ALIAS
    );

    @Override
    public @Nullable EObject getElementWithNameAt(XtextResource resource, int offset) {
        var element = super.getElementWithNameAt(resource, offset);
        if (element != null) {
            return element;
        }

        var declaration = resolveElementAt(resource, offset);
        if (declaration == null) {
            return null;
        }
        var leaf = NodeModelUtils.findLeafNodeAtOffset(resource.getParseResult().getRootNode(), offset);
        for (var name : DECLARED_NAMES) {
            if (name.getEContainingClass().isInstance(declaration)) {
                for (var node : NodeModelUtils.findNodesForFeature(declaration, name)) {
                    for (var nameLeaf : node.getLeafNodes()) {
                        if (nameLeaf.equals(leaf)) {
                            return declaration;
                        }
                    }
                }
            }
        }
        return null;
    }

}
