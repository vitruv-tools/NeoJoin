import * as vscode from "vscode";
import { ExtensionContext } from "vscode";
import { State } from "vscode-languageclient/node";

import * as lsp from "./lsp";
import { delay, getErrorMessage } from "./utils";
import * as visualization from "./visualization";
import VisualizationManager from "./visualization/manager";

function registerCommands(
    context: ExtensionContext,
    languageClient: lsp.NeoJoinLanguageClient,
    visualizationManager: VisualizationManager
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
                            increment: 0, // 0%
                            message: "Stopping ...",
                        });
                        await languageClient.stop();
                        if (DEV) {
                            // wait for the java agent port to be released
                            await delay(1000);
                        }
                        languageClient.reload();
                        progress.report({
                            increment: 50, // 50%
                            message: "Starting ...",
                        });
                        await languageClient.start();
                        visualizationManager.updateAll(true);
                        progress.report({ increment: 49, message: "Done" }); // 99%
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
    context.subscriptions.push(
        vscode.commands.registerCommand("neojoin.generate-viewtype", () => {
            if (languageClient.state !== State.Running) {
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

            languageClient.sendRequest<Response>("viewtype", {
                textDocument: languageClient.getDocumentId(currentEditor.document),
            }).catch (reason => {
                vscode.window.showErrorMessage("NeoJoin: " + reason.message);
            });
        })
    );
}

export async function activate(context: ExtensionContext) {
    try {
        const languageClient = await lsp.createLanguageClient(context);
        const visualizationManager = await visualization.activate(
            context,
            languageClient
        );
        registerCommands(context, languageClient, visualizationManager);

        if (DEV) {
            vscode.window.showInformationMessage(
                "NeoJoin initialized in development mode."
            );
        }
    } catch (e) {
        vscode.window.showErrorMessage(
            "Failed to initialize NeoJoin Plugin: " + getErrorMessage(e)
        );
    }
}

export function deactivate() {
    // everything registered via `context.subscriptions.push(...)` is disposed automatically
}
