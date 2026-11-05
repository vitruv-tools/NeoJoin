package tools.vitruv.neojoin.ide.visualization;

import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.jsonrpc.services.JsonSegment;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * Definition of the {@link VisualizationGenerator visualization} service for the <a href="https://github.com/eclipse-lsp4j/lsp4j/blob/main/documentation/jsonrpc.md">JSON-RPC framework</a>.
 */
@JsonSegment("visualization")
public interface VisualizationService {

    /**
     * Response for a visualization request.
     *
     * @param svg     the SVG string, or null if the generation failed
     * @param message a message in case of an _expected_ failure, or null if the generation was successful
     */
    record TargetModelResponse(
        @Nullable String svg,
        @Nullable String message
    ) {}

    /**
     * Optional parameters for the visualization generation.
     */
    record Options(
        boolean orthogonalArrows,
        boolean darkMode
    ) {}

    interface Params {

        @Nullable
        TextDocumentIdentifier textDocument();

        @Nullable
        Options options();

    }

    record SimpleParams(
        @Nullable TextDocumentIdentifier textDocument,
        @Nullable Options options
    ) implements Params {}

    @JsonRequest(value = "full")
    CompletableFuture<@Nullable TargetModelResponse> getFullVisualization(SimpleParams params);

    @JsonRequest(value = "referenced")
    CompletableFuture<@Nullable TargetModelResponse> getReferencedVisualization(SimpleParams params);

    record SelectedParams(
        @Nullable TextDocumentIdentifier textDocument,
        @Nullable Options options,
        @Nullable Range selection
    ) implements Params {}

    @JsonRequest(value = "selected")
    CompletableFuture<@Nullable TargetModelResponse> getSelectedVisualization(SelectedParams params);

}
