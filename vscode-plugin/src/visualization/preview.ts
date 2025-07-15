import * as vscode from "vscode";

import { VisualizationUpdateDebounceDelay } from "../constants";
import {
	escapeForHTML,
	getErrorMessage,
	getSelectionInDocument,
	isDarkThemeEnabled,
	promiseResult,
	replacePlaceholder,
} from "../utils";
import previewContentHTML from "./preview.html";
import VisualizationClient, { Mode, ModeType } from "./request";

enum MessageType {
	Ok = "ok",
	Warning = "warning",
	Error = "error",
}

const INITIAL_UPDATE_ID = -1;

export interface PreviewOptions {
	mode: ModeType;
	orthogonal: boolean;
}

export function createPreviewOptions(
	opts: Partial<PreviewOptions> = {}
): PreviewOptions {
	return {
		mode: opts?.mode ?? "referenced",
		orthogonal: opts?.orthogonal ?? false,
	};
}

function createPreviewContent(uri: vscode.Uri, options: PreviewOptions) {
	return replacePlaceholder(previewContentHTML, {
		URI: escapeForHTML(uri.toString()),
		MODE: options.mode,
		ORTHOGONAL: String(options.orthogonal),
	});
}

function getPreviewTitle(document: vscode.TextDocument) {
	const fileName = document.uri.path.split("/").pop();
	return `Preview: ${fileName}`;
}

let globalNextUpdateId = 0;

export default class Preview {
	#document: vscode.TextDocument;
	#panel: vscode.WebviewPanel;
	#pendingUpdateTimeout: NodeJS.Timeout | undefined = undefined;
	#lastUpdateId: number = INITIAL_UPDATE_ID;
	#options: PreviewOptions;
	#client: VisualizationClient;
	#onDestroy: () => void;

	get mode() {
		return this.#options.mode;
	}

	constructor(
		panel: vscode.WebviewPanel,
		document: vscode.TextDocument,
		options: PreviewOptions,
		client: VisualizationClient,
		onDestroy: () => void
	) {
		this.#document = document;
		this.#panel = panel;
		this.#options = options;
		this.#client = client;
		this.#onDestroy = onDestroy;

		panel.onDidDispose(() => {
			clearTimeout(this.#pendingUpdateTimeout);
			this.#onDestroy();
		});

		panel.webview.onDidReceiveMessage((message?: Record<string, any>) => {
			if (message?.command === "option") {
				(this.#options as any)[message.option] = message.value;
				this.update(true);
			}
		});

		panel.title = getPreviewTitle(document);
		panel.webview.html = createPreviewContent(document.uri, options);

		this.update(true);
	}

	update(instant: boolean) {
		if (instant) {
			clearTimeout(this.#pendingUpdateTimeout!);
			this.#doUpdate();
			return;
		}

		// debounce updates
		if (this.#pendingUpdateTimeout) {
			this.#pendingUpdateTimeout.refresh();
		} else {
			this.#pendingUpdateTimeout = setTimeout(
				() => this.#doUpdate(),
				VisualizationUpdateDebounceDelay
			);
		}
	}

	async #doUpdate() {
		this.#pendingUpdateTimeout = undefined;
		const updateId = globalNextUpdateId++;

		const mode = this.#getModeForRequest();
		if (mode === null) {
			this.#postUpdate(
				MessageType.Warning,
				"No selection available. Please focus the corresponding editor."
			);
			return;
		}

		const result = await this.#requestVisualization(mode);

		if (updateId < this.#lastUpdateId) {
			return; // outdated request
		}
		this.#lastUpdateId = updateId;

		if (result.status === "rejected") {
			const message = getErrorMessage(result.reason);
			if (message === "No query found at cursor position.") {
				this.#postUpdate(MessageType.Warning, message);
			} else if (message === "Timeout while rendering.") {
				this.#postUpdate(
					MessageType.Error,
					"Timeout while rendering. Consider using the 'Selected' mode for large models. Also note that timeouts can leak rendering processes, which can degrade system performance."
				);
			} else {
				console.error(
					"Error while trying to get visualization:",
					result.reason
				);
				this.#postUpdate(MessageType.Error, "Error: " + message);
			}
		} else {
			const type = result.value.message
				? MessageType.Warning
				: MessageType.Ok;
			this.#postUpdate(type, result.value.message, result.value.svg);
		}
	}

	#postUpdate(type: MessageType, message = "", svg?: string) {
		this.#panel.webview.postMessage({ type, message, svg });
	}

	#getModeForRequest(): Mode | null {
		if (this.#options.mode === "selected") {
			const selection = getSelectionInDocument(this.#document);
			if (selection) {
				return { type: "selected", selection };
			} else {
				return null; // no selection available
			}
		} else {
			return { type: this.#options.mode };
		}
	}

	#requestVisualization(mode: Mode) {
		return promiseResult(
			this.#client.getTargetModelVisualization(this.#document, mode, {
				orthogonalArrows: this.#options.orthogonal,
				darkMode: isDarkThemeEnabled(),
			})
		);
	}

	reveal() {
		this.#panel.reveal();
	}

	close() {
		this.#panel.dispose();
	}
}
