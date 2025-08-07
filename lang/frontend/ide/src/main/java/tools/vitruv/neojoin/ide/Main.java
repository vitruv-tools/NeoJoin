package tools.vitruv.neojoin.ide;

import com.google.inject.Guice;
import org.eclipse.emf.ecore.impl.EPackageRegistryImpl;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.xtext.ide.server.ServerLauncher;
import org.eclipse.xtext.ide.server.ServerModule;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import tools.vitruv.neojoin.collector.PackageModelCollector;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;

@NullUnmarked
@Command(name = "NeoJoin IDE", mixinStandardHelpOptions = true)
public class Main implements Runnable {

	@Option(names = {"-m", "--meta-model-path"}, paramLabel = "MODEL-PATH", required = true, description = "Path specification to find referenced meta-models.")
	String metaModelPath;

	@ArgGroup(exclusive = false)
	Logging logging;

	static class Logging {

		@Option(names = {"--log"}, paramLabel = "LOG-FILE", required = true, arity = "0..1", fallbackValue = "neojoin-ide-debug.log", description = "Enable logging.")
		Path logFile;

		@Option(names = {"--trace"}, description = "Enable trace logging for the language server (requires logging enabled).")
		boolean trace;

	}

	// vscode automatically appends an --stdio option, so we should not error when this option is encountered
	@SuppressWarnings("unused")
	@Option(names = {"--stdio"}, description = "Use stdio for communication (default, has no effect).")
	boolean stdio;

	@Override
	public void run() {
		var ioConfig = redirectIo();

		String errorMessage = null;
		try {
			NeoJoinIdeSetup.Registry = new PackageModelCollector(metaModelPath).collect();
		} catch (Exception ex) {
			NeoJoinIdeSetup.Registry = new EPackageRegistryImpl();
			errorMessage = "Invalid meta-model path: " + ex.getMessage();
			System.err.printf("[ERROR] %s%n  > full meta-model path: %s%n", errorMessage, metaModelPath);
		}

		System.out.println("Starting NeoJoin IDE server...");

		var languageServer = Guice.createInjector(new ServerModule()).getInstance(NeoJoinIdeServer.class);
		var launcher = Launcher.createLauncher(
			languageServer, LanguageClient.class,
			ioConfig.in(), ioConfig.out(),
			true, ioConfig.trace()
		);
		if (errorMessage != null) {
			launcher.getRemoteProxy().showMessage(new MessageParams(MessageType.Error, errorMessage));
		}
		languageServer.connect(launcher.getRemoteProxy());
		var future = launcher.startListening();

		System.out.println("NeoJoin IDE server started.");

		try {
			future.get(); // wait until the language server is stopped
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}
	}

	private record LanguageServerIoConfig(
		InputStream in,
		OutputStream out,
		@Nullable PrintWriter trace
	) {
	}

	/**
	 * Redirect standard IO because stdin and stdout are used for LSP communication.
	 */
	private LanguageServerIoConfig redirectIo() {
		var logStream = getLogOutputStream();

		var config = new LanguageServerIoConfig(
			System.in,
			System.out,
			logStream != null && isTraceLoggingEnabled() ? new PrintWriter(logStream, true) : null
		);

		System.setIn(ServerLauncher.silentIn());
		System.setOut(new PrintStream(
			logStream != null ? logStream : ServerLauncher.silentOut(),
			true
		));
		if (logStream != null) {
			// if there is a log file, also redirect stderr to keep all output in one place
			System.setErr(System.out);
		}

		return config;
	}

	private boolean isTraceLoggingEnabled() {
		return logging != null && logging.trace;
	}

	private @Nullable OutputStream getLogOutputStream() {
		if (logging == null) {
			return null;
		}

		try {
			return new FileOutputStream(logging.logFile.toFile());
		} catch (FileNotFoundException e) {
			throw new RuntimeException(
				"Failed to open log file (%s): %s%n".formatted(
					logging.logFile.toAbsolutePath().normalize(),
					e.getMessage()
				)
			);
		}
	}

	public static void main(String[] args) {
		System.exit(new CommandLine(new Main()).execute(args));
	}

}
