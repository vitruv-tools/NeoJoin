package tools.vitruv.neojoin.ide.custom;

import com.google.inject.Inject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.xtext.common.types.descriptions.ITypeDescriptor;
import org.eclipse.xtext.ide.editor.contentassist.ContentAssistContext;
import org.eclipse.xtext.ide.editor.contentassist.ContentAssistEntry;
import org.eclipse.xtext.ide.editor.contentassist.IdeContentProposalCreator;
import org.eclipse.xtext.naming.IQualifiedNameConverter;
import org.eclipse.xtext.util.ITextRegion;
import org.eclipse.xtext.xbase.ide.contentassist.ClasspathBasedIdeTypesProposalProvider;
import org.eclipse.xtext.xtype.XImportSection;

/**
 * Prevent Xbase code completion from trying to add java imports to the top of query files.
 */
public class CustomClasspathBasedIdeTypesProposalProvider extends ClasspathBasedIdeTypesProposalProvider {

    @Inject
    private IdeContentProposalCreator proposalCreator;

    @Inject
    private IQualifiedNameConverter qualifiedNameConverter;

    @Override
    protected ContentAssistEntry createProposal(
        EReference reference, ITypeDescriptor typeDesc,
        ContentAssistContext context, XImportSection importSection, ITextRegion importSectionRegion
    ) {
        String qualifiedName = qualifiedNameConverter.toString(typeDesc.getQualifiedName());
        return proposalCreator.createProposal(
            qualifiedName, context, (ContentAssistEntry it) -> {
                it.setKind(ContentAssistEntry.KIND_REFERENCE);
                it.setLabel(typeDesc.getSimpleName());
                it.setDescription(qualifiedName);
            }
        );
    }

}
