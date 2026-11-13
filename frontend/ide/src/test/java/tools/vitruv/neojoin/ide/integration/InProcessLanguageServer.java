package tools.vitruv.neojoin.ide.integration;

import com.google.inject.Guice;
import org.eclipse.lsp4j.ClientCapabilities;
import org.eclipse.lsp4j.DefinitionParams;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializedParams;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.MessageActionItem;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.ReferenceContext;
import org.eclipse.lsp4j.ReferenceParams;
import org.eclipse.lsp4j.ShowMessageRequestParams;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.WorkspaceClientCapabilities;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.xtext.ide.server.ServerModule;
import tools.vitruv.neojoin.collector.PackageModelCollector;
import tools.vitruv.neojoin.ide.NeoJoinIdeServer;
import tools.vitruv.neojoin.ide.NeoJoinIdeSetup;
import tools.vitruv.neojoin.ide.generation.ViewTypeGenerationService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A language server in this JVM with one workspace folder, which is also its meta-model path. Sends requests as an
 * editor does (like {@code ide.Main}, but without the stream connection).
 */
class InProcessLanguageServer {

    private final NeoJoinIdeServer server;
    private final RecordingClient client = new RecordingClient();

    private InProcessLanguageServer(NeoJoinIdeServer server) {
        this.server = server;
    }

    /**
     * Starts a server for the given directory: the meta-models in it are collected once, at the start.
     */
    static InProcessLanguageServer start(Path directory) throws ExecutionException, InterruptedException {
        NeoJoinIdeSetup.Registry = new PackageModelCollector(directory.toString()).collect().right();

        var server = Guice.createInjector(new ServerModule()).getInstance(NeoJoinIdeServer.class);
        var languageServer = new InProcessLanguageServer(server);
        server.connect(languageServer.client);

        // without the capability, Xtext ignores the workspace folders and needs the deprecated root URI
        var workspace = new WorkspaceClientCapabilities();
        workspace.setWorkspaceFolders(true);
        var capabilities = new ClientCapabilities();
        capabilities.setWorkspace(workspace);
        var params = new InitializeParams();
        params.setCapabilities(capabilities);
        params.setWorkspaceFolders(List.of(new WorkspaceFolder(directory.toUri().toString(), "workspace")));
        server.initialize(params).get();
        server.initialized(new InitializedParams());
        return languageServer;
    }

    /**
     * Writes the file and opens it in the editor.
     *
     * @return URI of the file
     */
    String open(Path file, String text) throws IOException {
        Files.writeString(file, text);
        var uri = file.toUri().toString();
        server.getTextDocumentService().didOpen(
            new DidOpenTextDocumentParams(new TextDocumentItem(uri, "neojoin", 1, text))
        );
        return uri;
    }

    List<Location> definition(String uri, Position position) throws ExecutionException, InterruptedException {
        var params = new DefinitionParams(new TextDocumentIdentifier(uri), position);
        return List.copyOf(server.getTextDocumentService().definition(params).get().getLeft());
    }

    /**
     * Returns the references including the declaration, as VS Code requests them.
     */
    List<Location> references(String uri, Position position) throws ExecutionException, InterruptedException {
        var params = new ReferenceParams(new TextDocumentIdentifier(uri), position, new ReferenceContext(true));
        return List.copyOf(server.getTextDocumentService().references(params).get());
    }

    /**
     * Returns the first diagnostics that the server publishes for the file (it validates after the start and after
     * opening a file).
     */
    List<Diagnostic> diagnostics(String uri) throws ExecutionException, InterruptedException, TimeoutException {
        return client.diagnosticsOf(uri).get(10, TimeUnit.SECONDS);
    }

    void generateViewType(String uri) throws ExecutionException, InterruptedException {
        server.generateViewType(new ViewTypeGenerationService.Params(new TextDocumentIdentifier(uri))).get();
    }

    private static class RecordingClient implements LanguageClient {

        private final Map<String, CompletableFuture<List<Diagnostic>>> diagnostics = new ConcurrentHashMap<>();

        CompletableFuture<List<Diagnostic>> diagnosticsOf(String uri) {
            return diagnostics.computeIfAbsent(uri, key -> new CompletableFuture<>());
        }

        @Override
        public void publishDiagnostics(PublishDiagnosticsParams params) {
            diagnosticsOf(params.getUri()).complete(List.copyOf(params.getDiagnostics()));
        }

        @Override
        public void telemetryEvent(Object object) {
        }

        @Override
        public void showMessage(MessageParams messageParams) {
        }

        @Override
        public CompletableFuture<MessageActionItem> showMessageRequest(ShowMessageRequestParams requestParams) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void logMessage(MessageParams message) {
        }

    }

}
