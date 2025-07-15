import * as vscode from "vscode";

import { VisualizationPanelName } from "../constants";
import Preview, { createPreviewOptions, PreviewOptions } from "./preview";
import VisualizationClient from "./request";

// restore preview from previous session
class PreviewSerializer implements vscode.WebviewPanelSerializer {
	#manager: PreviewManager;

	constructor(manager: PreviewManager) {
		this.#manager = manager;
	}

	async deserializeWebviewPanel(
		panel: vscode.WebviewPanel,
		state: any
	): Promise<void> {
		const document = vscode.workspace.textDocuments.find(
			(doc) => doc.uri.toString() === state?.uri
		);
		if (document) {
			this.#manager.restore(
				panel,
				document,
				createPreviewOptions(state?.options)
			);
		} else {
			panel.dispose();
		}
	}
}

export default class PreviewManager {
	#client: VisualizationClient;
	#activePreviews = new Map<vscode.TextDocument, Preview>();

	constructor(client: VisualizationClient) {
		this.#client = client;
	}

	show() {
		if (!this.#client.ready) {
			vscode.window.showErrorMessage(
				"NeoJoin: Language server not ready"
			);
			return;
		}

		const currentEditor = vscode.window.activeTextEditor;
		if (!currentEditor) {
			vscode.window.showErrorMessage("NeoJoin: No active editor");
			return;
		}

		if (currentEditor.document.languageId !== "neojoin") {
			vscode.window.showErrorMessage("NeoJoin: Not a NeoJoin query file");
			return;
		}

		const existingPreview = this.#activePreviews.get(
			currentEditor.document
		);
		if (existingPreview) {
			existingPreview.reveal();
			return;
		}

		const panel = vscode.window.createWebviewPanel(
			VisualizationPanelName,
			"Preview", // is immediately replaced
			vscode.ViewColumn.Beside,
			{ enableScripts: true }
		);

		this.restore(panel, currentEditor.document, createPreviewOptions());
	}

	restore(
		panel: vscode.WebviewPanel,
		document: vscode.TextDocument,
		options: PreviewOptions
	) {
		const preview = new Preview(
			panel,
			document,
			options,
			this.#client,
			() => this.#activePreviews.delete(document)
		);
		this.#activePreviews.set(document, preview);
	}

	update(document: vscode.TextDocument, instant: boolean) {
		const preview = this.#activePreviews.get(document);
		if (preview) {
			preview.update(instant);
		}
	}

	updateAll(instant: boolean) {
		this.#activePreviews.forEach((preview) => preview.update(instant));
	}

	updateIfInSelectedMode(document: vscode.TextDocument) {
		const preview = this.#activePreviews.get(document);
		if (preview && preview.mode === "selected") {
			preview.update(false);
		}
	}

	close(document: vscode.TextDocument) {
		const preview = this.#activePreviews.get(document);
		if (preview) {
			preview.close();
		}
	}

	closeAll() {
		this.#activePreviews.forEach((preview) => preview.close());
	}

	serializer() {
		return new PreviewSerializer(this);
	}
}
