import * as vscode from "vscode";
import { ExtensionContext } from "vscode";

import { VisualizationPanelName } from "../constants";
import { NeoJoinLanguageClient } from "../lsp";
import VisualizationManager from "./manager";
import VisualizationClient from "./request";

/**
 * Activates the visualization feature.
 */
export async function activate(
	context: ExtensionContext,
	languageClient: NeoJoinLanguageClient
) {
	const manager = new VisualizationManager(
		new VisualizationClient(languageClient)
	);

	context.subscriptions.push(
		vscode.commands.registerCommand("neojoin.visualization", () =>
			manager.show()
		),
		vscode.workspace.onDidCloseTextDocument((doc) => manager.close(doc)),
		vscode.workspace.onDidChangeTextDocument((event) => {
			// check whether text content has changed
			if (event.contentChanges.length > 0) {
				manager.update(event.document, false);
			}
		}),
		vscode.window.onDidChangeTextEditorSelection((event) => {
			if (event.textEditor.document.languageId === "neojoin") {
				manager.updateIfInSelectedMode(event.textEditor.document);
			}
		}),
		vscode.window.onDidChangeActiveColorTheme(() =>
			manager.updateAll(true)
		),
		// for restoring panels after restart
		vscode.window.registerWebviewPanelSerializer(
			VisualizationPanelName,
			manager.serializer()
		),
		// close all panels when the extension is deactivated
		new vscode.Disposable(() => manager.closeAll())
	);

	return manager;
}
