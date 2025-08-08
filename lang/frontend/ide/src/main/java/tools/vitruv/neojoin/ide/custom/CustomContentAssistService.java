package tools.vitruv.neojoin.ide.custom;

import com.google.inject.Singleton;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.xtext.ide.editor.contentassist.ContentAssistEntry;
import org.eclipse.xtext.ide.editor.contentassist.antlr.LeafNodeFinder;
import org.eclipse.xtext.ide.server.Document;
import org.eclipse.xtext.ide.server.contentassist.ContentAssistService;
import org.eclipse.xtext.nodemodel.ILeafNode;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.util.CancelIndicator;
import org.jspecify.annotations.Nullable;

/**
 * Customize how code completion handles completion requests in the middle of text.
 * <p>
 * Example: Code completion is triggered in
 * <br><code>import "http://exam|asdf"</code><br>
 * at the location indicated by {@code |} and the first result is accepted. By default,
 * this leads to
 * <br><code>import "http://example.org/model"asdf"</code><br>
 * which contains syntax errors and is clearly not intended. This custom implementation instead
 * produces the desired result:
 * <br><code>import "http://example.org/model"</code><br>
 */
@Singleton
public class CustomContentAssistService extends ContentAssistService {

    private @Nullable Position findCurrentNode(Document document, XtextResource resource, CompletionParams params) {
        Position caretPosition = params.getPosition();
        int caretOffset = document.getOffSet(caretPosition);
        var rootNode = resource.getParseResult().getRootNode();

        ILeafNode currentNode = new LeafNodeFinder(caretOffset, true).searchIn(rootNode);
        if (currentNode == null || currentNode.isHidden()) {
            currentNode = new LeafNodeFinder(caretOffset, false).searchIn(rootNode);
        }

        if (currentNode != null && !currentNode.isHidden()) {
            return document.getPosition(currentNode.getEndOffset());
        } else {
            return null;
        }
    }

    private @Nullable Position endPosition;

    @Override
    public CompletionList createCompletionList(
        Document document,
        XtextResource resource,
        CompletionParams params,
        CancelIndicator cancelIndicator
    ) {
        try {
            endPosition = findCurrentNode(document, resource, params);
            return super.createCompletionList(document, resource, params, cancelIndicator);
        } finally {
            endPosition = null;
        }
    }

    @Override
    protected CompletionItem toCompletionItem(
        ContentAssistEntry entry,
        int caretOffset,
        Position caretPosition,
        Document document
    ) {
        var item = super.toCompletionItem(entry, caretOffset, caretPosition, document);

        if (endPosition != null && item.getTextEdit().getLeft() != null) {
            item.getTextEdit().getLeft().getRange().setEnd(endPosition);
        }

        return item;
    }

}
