package tools.vitruv.neojoin.ide.custom;

import org.eclipse.xtext.CrossReference;
import org.eclipse.xtext.RuleCall;
import org.eclipse.xtext.ide.editor.contentassist.ContentAssistContext;
import org.eclipse.xtext.ide.editor.contentassist.ContentAssistEntry;
import org.eclipse.xtext.ide.editor.contentassist.IdeCrossrefProposalProvider;
import org.eclipse.xtext.resource.IEObjectDescription;

/**
 * Surround completion proposals for strings in double quotes. See <a href="https://github.com/eclipse-xtext/xtext/discussions/3375">#3375</a>.
 */
public class CustomIdeCrossrefProposalProvider extends IdeCrossrefProposalProvider {

    @Override
    protected ContentAssistEntry createProposal(
        IEObjectDescription candidate, CrossReference crossRef,
        ContentAssistContext context
    ) {
        var qualifiedName = getQualifiedNameConverter().toString(candidate.getName());
        if (crossRef.getTerminal() instanceof RuleCall call) {
            if (call.getRule().getName().equals("STRING")) {
                qualifiedName = '"' + qualifiedName + '"';
            }
        }
        return getProposalCreator().createProposal(
            qualifiedName, context, (e) -> {
                e.setSource(candidate);
                e.setDescription(candidate.getEClass() != null ? candidate.getEClass().getName() : null);
                e.setKind(ContentAssistEntry.KIND_REFERENCE);
            }
        );
    }

}
