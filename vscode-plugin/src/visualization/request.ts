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
	svg?: string
	message?: string
}

export default class VisualizationClient {
	#languageClient: NeoJoinLanguageClient;

	constructor(languageClient: NeoJoinLanguageClient) {
		this.#languageClient = languageClient;
	}

	get ready() {
		return this.#languageClient.state === State.Running;
	}

	#request<T>(method: string, param?: any): Promise<T> {
		if (!this.ready) {
			throw new Error("Visualization client not available");
		}
		return this.#languageClient.sendRequest(method, param);
	}

	async getTargetModelVisualization(
		doc: vscode.TextDocument,
		mode: Mode,
		options: Options
	) {
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
