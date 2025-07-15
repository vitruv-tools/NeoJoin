
import { homedir } from 'os';
import { resolve as resolvePath } from 'path';
import * as vscode from "vscode";
import { ExtensionContext } from "vscode";
import {
	LanguageClient,
	LanguageClientOptions,
	State,
	TextDocumentIdentifier,
	TransportKind,
} from "vscode-languageclient/node";

import config from "../config";

export async function activate(context: ExtensionContext) {
	const client = new NeoJoinLanguageClient(context.extensionPath);
	await client.start();
	context.subscriptions.push(client);
	return client;
}

interface ServerConfig {
	cwd: string;
	jar: string;
	mmPath: string;
	jvmOpts: string[];
	programOpts: string[];
}

function debugLogFilePath(): string {
	const workspaceFolder = vscode.workspace.workspaceFolders?.[0];
	if (workspaceFolder) {
		return resolvePath(workspaceFolder.uri.fsPath, "neojoin-debug.log");
	} else {
		return resolvePath(homedir(), "neojoin-debug.log");
	}
}

function createConfig(extensionDir: string): ServerConfig {
	if (DEBUG) {
		return {
			cwd: extensionDir,
			jar: resolvePath(extensionDir, "../lang/frontend/ide/target/tools.vitruv.neojoin.frontend.ide.jar"),
			mmPath: config.metaModelSearchPath,
			jvmOpts: [
				"-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,quiet=y,address=*:8000", // set suspend=y to wait for debugger on launch
			],
			programOpts: ["--trace", "--log", "neojoin-debug.log"], // log is placed in /path/to/neojoin/vscode-plugin/neojoin-debug.log
		};
	} else {
		return {
			cwd: extensionDir,
			jar: resolvePath(extensionDir, "tools.vitruv.neojoin.frontend.ide.jar"),
			mmPath: config.metaModelSearchPath,
			jvmOpts: [],
			programOpts: config.debug ? ["--trace", "--log", debugLogFilePath()] : [],
		};
	}
}

const CLIENT_OPTIONS: LanguageClientOptions = {
	documentSelector: [{ scheme: "file", language: "neojoin" }],
	connectionOptions: {
		maxRestartCount: 1,
	},
};

export class NeoJoinLanguageClient {
	#extensionDir: string;
	#client: LanguageClient | null = null;

	constructor(extensionDir: string) {
		this.#extensionDir = extensionDir;
	}

	#buildServerOptions() {
		const config = createConfig(this.#extensionDir);
		return {
			command: "java",
			transport: TransportKind.stdio,
			args: [
				...config.jvmOpts,
				"-jar",
				config.jar,
				"--meta-model-path",
				config.mmPath,
				...config.programOpts,
			],
			options: {
				cwd: config.cwd,
			},
		};
	}

	get state() {
		return this.#client !== null ? this.#client.state : State.Stopped;
	}

	public async start() {
		if (
			this.#client === null ||
			(this.#client as any).$state === "startFailed"
		) {
			this.#client = new LanguageClient(
				"neojoin-lsp",
				"NeoJoin Language Server",
				this.#buildServerOptions(),
				CLIENT_OPTIONS
			);
		}

		await this.#client.start();
	}

	public async stop() {
		if (this.state === State.Stopped) {
			return;
		}

		await this.#client!.stop();
		this.#client = null;
	}

	public dispose() {
		if (this.#client !== null) {
			return this.#client.dispose();
		}
	}

	public reload() {
		if (this.state !== State.Stopped) {
			throw new Error("Cannot reload while server is running");
		}
		this.#client = null;
	}

	public sendRequest<T>(method: string, param?: any): Promise<T> {
		if (this.#client === null) {
			throw new Error("Language client not started");
		}
		return this.#client.sendRequest(method, param);
	}

	public getDocumentId(doc: vscode.TextDocument): TextDocumentIdentifier {
		if (this.#client === null) {
			throw new Error("Language client not started");
		}

		return this.#client.code2ProtocolConverter.asTextDocumentIdentifier(
			doc
		);
	}
}
