import * as vscode from "vscode";
import { State } from "vscode-languageclient";

import { NeoJoinLanguageClient } from "../lsp";

export type Mode =
	| { type: "full" }
	| { type: "referenced" }
	| { type: "selected"; selection: vscode.Selection };
export type ModeType = Mode["type"];
const MODES: ModeType[] = ["full", "referenced", "selected"];

interface Options {
	orthogonalArrows?: boolean;
	darkMode?: boolean;
}

interface Response {
	// visualization as SVG string
	svg?: string;
	// warning message
	message?: string;
}

/**
 * Wrapper around the NeoJoin language client that provides methods for requesting visualizations.
 */
export default class VisualizationClient {
	#languageClient: NeoJoinLanguageClient;

	constructor(languageClient: NeoJoinLanguageClient) {
		this.#languageClient = languageClient;
	}

	/**
	 * Returns whether the visualization client is ready to handle requests.
	 */
	get ready() {
		return this.#languageClient.state === State.Running;
	}

	#request<T>(method: string, param?: any): Promise<T> {
		if (!this.ready) {
			throw new Error("Visualization client not available");
		}
		return this.#languageClient.sendRequest(method, param);
	}

	/**
	 * Requests a visualization for the given document.
	 * @param doc open document
	 * @param mode visualization mode
	 * @param options options
	 * @returns promise that resolves with a visualization response
	 */
	getTargetModelVisualization(
		doc: vscode.TextDocument,
		mode: Mode,
		options: Options
	): Promise<Response> {
		if (!MODES.includes(mode.type)) {
			throw new Error("Invalid mode: " + mode.type);
		}
		return this.#request<Response>("visualization/" + mode.type, {
			textDocument: this.#languageClient.getDocumentId(doc),
			selection: mode.type === "selected" ? mode.selection : undefined,
			options,
		});
	}
}
