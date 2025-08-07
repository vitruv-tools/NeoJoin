import * as vscode from "vscode";

import { VisualizationPanelName } from "../constants";
import VisualizationPanel, { createPanelOptions, PanelOptions } from "./panel";
import VisualizationClient from "./request";

/**
 * Serializer for restoring visualization panels after a restart of VS Code.
 */
class VisualizationPanelSerializer implements vscode.WebviewPanelSerializer {
	#manager: VisualizationManager;

	constructor(manager: VisualizationManager) {
		this.#manager = manager;
	}

	async deserializeWebviewPanel(
		webview: vscode.WebviewPanel,
		state: any
	): Promise<void> {
		const document = vscode.workspace.textDocuments.find(
			(doc) => doc.uri.toString() === state?.uri
		);
		if (document) {
			this.#manager.restore(
				webview,
				document,
				createPanelOptions(state?.options)
			);
		} else {
			// cannot find a corresponding open document, so close the panel
			webview.dispose();
		}
	}
}

/**
 * Manages multiple open visualization panels.
 */
export default class VisualizationManager {
	#client: VisualizationClient;
	#activePanels = new Map<vscode.TextDocument, VisualizationPanel>();

	constructor(client: VisualizationClient) {
		this.#client = client;
	}

	/**
	 * Show a visualization panel for the currently active document.
	 */
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

		const existingPanel = this.#activePanels.get(currentEditor.document);
		if (existingPanel) {
			existingPanel.reveal();
			return;
		}

		const panel = vscode.window.createWebviewPanel(
			VisualizationPanelName,
			"Preview", // is immediately replaced
			vscode.ViewColumn.Beside,
			{ enableScripts: true }
		);

		this.restore(panel, currentEditor.document, createPanelOptions());
	}

	/**
	 * Restore a visualization panel after a restart of VS Code.
	 * @param webview webview panel to restore content for
	 * @param document the document to visualize
	 * @param options options for the panel
	 */
	restore(
		webview: vscode.WebviewPanel,
		document: vscode.TextDocument,
		options: PanelOptions
	) {
		const panel = new VisualizationPanel(
			webview,
			document,
			options,
			this.#client,
			() => this.#activePanels.delete(document)
		);
		this.#activePanels.set(document, panel);
	}

	/**
	 * Update the visualization corresponding to the given document if it exists.
	 * @param document document to update the corresponding visualization for
	 * @param instant whether to update immediately or debounce the update
	 */
	update(document: vscode.TextDocument, instant: boolean) {
		const panel = this.#activePanels.get(document);
		if (panel) {
			panel.update(instant);
		}
	}

	/**
	 * Update all active visualizations.
	 * @param instant whether to update immediately or debounce the update
	 */
	updateAll(instant: boolean) {
		this.#activePanels.forEach((panel) => panel.update(instant));
	}

	/**
	 * Update the visualization corresponding to the given document if it is in "selected" mode.
	 * @param document document to update the corresponding visualization for
	 */
	updateIfInSelectedMode(document: vscode.TextDocument) {
		const panel = this.#activePanels.get(document);
		if (panel && panel.mode === "selected") {
			panel.update(false);
		}
	}

	/**
	 * Close the visualization panel for the given document if it exists.
	 * @param document document to close the corresponding visualization for
	 */
	close(document: vscode.TextDocument) {
		const panel = this.#activePanels.get(document);
		if (panel) {
			panel.close();
		}
	}

	/**
	 * Close all active visualization panels.
	 */
	closeAll() {
		this.#activePanels.forEach((panel) => panel.close());
	}

	/**
	 * Get a {@link WebviewPanelSerializer} to restore panels after a restart of VS Code.
	 */
	serializer() {
		return new VisualizationPanelSerializer(this);
	}
}
