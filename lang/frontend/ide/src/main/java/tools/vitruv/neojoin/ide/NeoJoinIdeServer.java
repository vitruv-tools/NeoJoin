package tools.vitruv.neojoin.ide;

import com.google.inject.Inject;
import net.sourceforge.plantuml.FileFormat;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.ResponseErrorException;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseError;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseErrorCode;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.ide.server.Document;
import org.eclipse.xtext.ide.server.LanguageServerImpl;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.service.OperationCanceledManager;
import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.xbase.lib.Functions.Function3;
import org.jspecify.annotations.Nullable;
import tools.vitruv.neojoin.aqr.AQR;
import tools.vitruv.neojoin.aqr.AQRBuilder;
import tools.vitruv.neojoin.aqr.AQRInvariantViolatedException;
import tools.vitruv.neojoin.ast.Query;
import tools.vitruv.neojoin.ast.ViewTypeDefinition;
import tools.vitruv.neojoin.generation.MetaModelGenerator;
import tools.vitruv.neojoin.generation.ModelInfo;
import tools.vitruv.neojoin.ide.generation.ViewTypeGenerationService;
import tools.vitruv.neojoin.ide.visualization.PlantUMLRenderer;
import tools.vitruv.neojoin.ide.visualization.VisualizationGenerator;
import tools.vitruv.neojoin.ide.visualization.VisualizationService;
import tools.vitruv.neojoin.jvmmodel.ExpressionHelper;
import tools.vitruv.neojoin.utils.AstUtils;
import tools.vitruv.neojoin.utils.EMFUtils;
import tools.vitruv.neojoin.utils.Mutable;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static tools.vitruv.neojoin.utils.Assertions.check;
import static tools.vitruv.neojoin.utils.Assertions.fail;

public class NeoJoinIdeServer extends LanguageServerImpl implements VisualizationService, ViewTypeGenerationService {

    @Inject
    OperationCanceledManager cancellation;

    private static ResponseErrorException makeResponseErrorException(String reason) {
        return new ResponseErrorException(new ResponseError(ResponseErrorCode.RequestFailed, reason, null));
    }

    private static void checkRequiredParameter(@Nullable Object value, String parameter) {
        if (value == null) {
            throw makeResponseErrorException("Missing required parameter: " + parameter);
        }
    }

    private ExpressionHelper getExpressionHelper(URI uri) {
        return getService(getResourceServiceProvider(uri), ExpressionHelper.class);
    }

    /**
     * Read from a given managed document.
     *
     * @param uri       uri of the document
     * @param operation read operation
     * @param <T>       result type
     * @return future for the result of the read operation
     */
    private <T extends @Nullable Object> CompletableFuture<T> read(
        URI uri,
        Function3<@Nullable Document, @Nullable XtextResource, CancelIndicator, T> operation
    ) {
        return getRequestManager().runRead(cancelIndicator -> getWorkspaceManager().doRead(
            uri,
            (doc, res) -> operation.apply(doc, res, cancelIndicator)
        ));
    }

    /**
     * Read the given document and generate an AQR representation.
     *
     * @param uri    uri of the document
     * @param onRead optional additional read operations
     * @return future for the AQR or {@code null} if the query had errors
     */
    private CompletableFuture<@Nullable AQR> readAQR(
        URI uri,
        BiConsumer<Document, XtextResource> onRead
    ) {
        return read(
            uri, (doc, res, cancelIndicator) -> {
                if (doc == null || res == null) {
                    throw makeResponseErrorException("Document not found: " + uri);
                }

                if (res.getContents().isEmpty() || !(res.getContents().get(0) instanceof ViewTypeDefinition)) {
                    throw makeResponseErrorException("Not a NeoJoin query: " + uri);
                }

                if (!res.getErrors().isEmpty()) {
                    // can only generate AQR from valid query because ast model might be incomplete otherwise
                    // however, this does not catch all errors, so query might still be invalid
                    return null;
                }

                AQR aqr;
                try {
                    aqr = new AQRBuilder(
                        (ViewTypeDefinition) res.getContents().get(0),
                        getExpressionHelper(uri)
                    ).build();
                } catch (AQRInvariantViolatedException ex) { // invalid query
                    return null;
                }
                cancellation.checkCanceled(cancelIndicator);

                onRead.accept(doc, res);
                cancellation.checkCanceled(cancelIndicator);

                return aqr;
            }
        );
    }

    private CompletableFuture<TargetModelResponse> makeResponse(
        VisualizationService.Params params,
        BiConsumer<Document, XtextResource> onRead,
        Function<ModelInfo, VisualizationGenerator.Mode> getMode
    ) {
        var uri = getURI(params.textDocument());
        var options = getOptions(params);
        return readAQR(uri, onRead).thenApply(aqr -> {
            if (aqr == null) {
                return new TargetModelResponse(null, "Cannot update the preview while the query has errors");
            } else {
                var targetMetaModel = new MetaModelGenerator(aqr).generate();
                checkError(targetMetaModel.diagnostic());

                var plantUMLSource = new VisualizationGenerator(
                    aqr,
                    targetMetaModel,
                    getMode.apply(targetMetaModel),
                    options.orthogonalArrows()
                ).generate();
                var svg = renderWithTimeout(plantUMLSource, options);
                return new TargetModelResponse(svg, formatIssues(targetMetaModel.diagnostic()));
            }
        });
    }

    private void checkError(Diagnostic diagnostic) {
        if ((diagnostic.getSeverity() & Diagnostic.ERROR) == 0) {
            return;
        }

        for (var child : diagnostic.getChildren()) {
            if ((child.getSeverity() & Diagnostic.ERROR) != 0) {
                throw makeResponseErrorException(child.getMessage());
            }
        }

        throw makeResponseErrorException(diagnostic.getMessage()); // fallback to root message
    }

    private @Nullable String formatIssues(Diagnostic diagnostic) {
        if (diagnostic.getSeverity() == Diagnostic.OK) {
            return null;
        }

        return diagnostic.getChildren().stream()
            .map(d -> "[%s] %s".formatted(EMFUtils.diagnosticSeverityText(d), d.getMessage()))
            .collect(Collectors.joining("\n"));
    }

    private final ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * PlantUML rendering can take an excessive amount of time for large models. At this point the visualization becomes
     * pretty useless anyway, so we stop waiting after a timeout of 10 seconds to at least show the user what the problem is.
     *
     * @implNote The timeout does not abort rendering, it just stops waiting for the result. So this method leaks rendering
     * threads and the started dot executable can even outlive the program itself.
     */
    private String renderWithTimeout(String plantUMLSource, Options options) {
        try {
            var task = executor.submit(() -> new PlantUMLRenderer(
                plantUMLSource,
                FileFormat.SVG,
                options.darkMode()
            ).renderToString());
            return task.get(10, TimeUnit.SECONDS);
        } catch (TimeoutException ex) {
            throw makeResponseErrorException("Timeout while rendering.");
        } catch (ExecutionException e) {
            throw makeResponseErrorException("Failed to render PlantUML: " + e.getCause().getMessage());
        } catch (InterruptedException e) {
            throw makeResponseErrorException("Rendering was interrupted.");
        }
    }

    private CompletableFuture<TargetModelResponse> makeResponse(VisualizationService.Params params, VisualizationGenerator.Mode mode) {
        return makeResponse(params, (doc, res) -> {}, targetMetaModel -> mode);
    }

    private static Options getOptions(VisualizationService.Params params) {
        Options options = params.options();
        return options != null ? options : new Options(false, false);
    }

    @Override
    public CompletableFuture<@Nullable TargetModelResponse> getFullVisualization(SimpleParams params) {
        checkRequiredParameter(params.textDocument(), "textDocument");
        return makeResponse(params, new VisualizationGenerator.Mode.Full());
    }

    @Override
    public CompletableFuture<@Nullable TargetModelResponse> getReferencedVisualization(SimpleParams params) {
        checkRequiredParameter(params.textDocument(), "textDocument");
        return makeResponse(params, new VisualizationGenerator.Mode.Referenced());
    }

    @Override
    public CompletableFuture<@Nullable TargetModelResponse> getSelectedVisualization(SelectedParams params) {
        checkRequiredParameter(params.textDocument(), "textDocument");
        checkRequiredParameter(params.selection(), "selection");
        var selectedQueryNames = new Mutable<@Nullable List<String>>(null);
        return makeResponse(
            params,
            (doc, res) -> {
                selectedQueryNames.value = getTargetClassNamesAtPosition(doc, res, params.selection());
            },
            targetMetaModel -> {
                check(selectedQueryNames.value != null);
                var selectedClasses = selectedQueryNames.value.stream().map(targetName -> {
                    var classifier = targetMetaModel.pack().getEClassifier(targetName);
                    return classifier instanceof EClass clazz ? clazz : fail("Cannot find target class: " + targetName);
                }).toList();
                return new VisualizationGenerator.Mode.Selected(selectedClasses);
            }
        );
    }

    @Override
    public CompletableFuture<Void> generateViewType(ViewTypeGenerationService.Params params) {
        var uri = getURI(params.textDocument());
        return readAQR(uri, (doc, res) -> {}).thenApply(aqr -> {
            if (aqr == null) {
                throw makeResponseErrorException("Cannot generate the view type while the query has errors");
            }

            ModelInfo result = new MetaModelGenerator(aqr).generate();
            checkError(result.diagnostic());

            try {
                EMFUtils.save(URI.createURI(params.textDocument().getUri() + ".ecore"), result.pack());
            } catch (IOException e) {
                throw makeResponseErrorException("Cannot save view type file");
            }
            return null;
        });
    }

    /**
     * Calculate which queries overlap with the given text selection in the document and
     * returns the names of the corresponding target classes.
     */
    private List<String> getTargetClassNamesAtPosition(Document doc, XtextResource res, Range selection) {
        var expressionHelper = getExpressionHelper(res.getURI());
        var startOffset = doc.getOffSet(selection.getStart());
        var endOffset = doc.getOffSet(selection.getEnd());
        var queriesInSelection = EcoreUtil2.getAllContentsOfType(res, Query.class).stream()
            .filter(query -> {
                var node = NodeModelUtils.getNode(query);
                return startOffset <= node.getEndOffset() && node.getOffset() <= endOffset;
            })
            .map(q -> AstUtils.getTargetName(q, expressionHelper))
            .toList();

        if (queriesInSelection.isEmpty()) {
            throw makeResponseErrorException("No query found at cursor position.");
        }

        return queriesInSelection;
    }

}
