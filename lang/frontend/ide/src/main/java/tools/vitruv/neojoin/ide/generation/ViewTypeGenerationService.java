package tools.vitruv.neojoin.ide.generation;

import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;

public interface ViewTypeGenerationService {

    record Params(
        TextDocumentIdentifier textDocument
    ) {}

    @JsonRequest("viewtype")
    CompletableFuture<Void> generateViewType(Params params);
    
}
