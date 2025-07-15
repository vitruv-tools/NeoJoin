import * as vscode from "vscode";
import { ExtensionContext } from "vscode";

import { VisualizationPanelName } from "../constants";
import PreviewManager from "./manager";
import VisualizationClient from "./request";
import { NeoJoinLanguageClient } from "../lsp";

export async function activate(
	context: ExtensionContext,
	languageClient: NeoJoinLanguageClient
) {
	const manager = new PreviewManager(new VisualizationClient(languageClient));

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
		vscode.window.registerWebviewPanelSerializer(
			VisualizationPanelName,
			manager.serializer()
		),
		new vscode.Disposable(() => manager.closeAll())
	);
}
