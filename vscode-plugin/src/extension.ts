import * as vscode from "vscode";
import { ExtensionContext } from "vscode";
import { State } from "vscode-languageclient/node";

import * as lsp from "./lsp";
import { delay, getErrorMessage } from "./utils";
import * as visualization from "./visualization";

function registerCommands(
	context: ExtensionContext,
	languageClient: lsp.NeoJoinLanguageClient
) {
	context.subscriptions.push(
		vscode.commands.registerCommand("neojoin.restart", () => {
			if (languageClient.state === State.Starting) {
				vscode.window.showErrorMessage(
					"Cannot restart NeoJoin: Server is currently starting."
				);
				return;
			}

			return vscode.window.withProgress(
				{
					location: vscode.ProgressLocation.Window,
					title: "Restarting NeoJoin",
					cancellable: false,
				},
				async (progress, _) => {
					try {
						progress.report({
							increment: 0,
							message: "Stopping ...",
						});
						await languageClient.stop();
						if (DEBUG) {
							// wait for the java agent port to be released
							await delay(1000);
						}
						languageClient.reload();
						progress.report({
							increment: 50,
							message: "Starting ...",
						});
						await languageClient.start();
						progress.report({ increment: 49, message: "Done" });
						await delay(1000);
					} catch (e) {
						vscode.window.showErrorMessage(
							"Failed to restart NeoJoin: " + getErrorMessage(e)
						);
					}
				}
			);
		})
	);
}

export async function activate(context: ExtensionContext) {
	try {
		const languageClient = await lsp.activate(context);
		await visualization.activate(context, languageClient);
		registerCommands(context, languageClient);
	} catch (e) {
		vscode.window.showErrorMessage(
			"Failed to initialize NeoJoin Plugin: " + getErrorMessage(e)
		);
	}

	// TODO remove
	vscode.window.showInformationMessage(
		`NeoJoin initialized${DEBUG ? " (debug)" : ""}`
	);
}

export function deactivate() {}
