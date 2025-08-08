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
import panelContentHTML from "./panel.html";
import VisualizationClient, { Mode, ModeType } from "./request";

/**
 * Type of messages sent from the {@link VisualizationPanel} to the webview.
 */
enum MessageType {
    Ok = "ok",
    Warning = "warning",
    Error = "error",
}

export interface PanelOptions {
    mode: ModeType;
    orthogonal: boolean;
}

export function createPanelOptions(
    opts: Partial<PanelOptions> = {}
): PanelOptions {
    return {
        mode: opts?.mode ?? "referenced",
        orthogonal: opts?.orthogonal ?? false,
    };
}

/**
 * Create the HTML content for the visualization panel webview based on the template in `panel.html`.
 * @param uri uri of the document to visualize
 * @param options options for the visualization
 * @returns HTML content for the webview
 */
function createPanelContent(uri: vscode.Uri, options: PanelOptions) {
    return replacePlaceholder(panelContentHTML, {
        URI: escapeForHTML(uri.toString()),
        MODE: options.mode,
        ORTHOGONAL: String(options.orthogonal),
    });
}

function getPanelTitle(document: vscode.TextDocument) {
    const fileName = document.uri.path.split("/").pop();
    return `Preview: ${fileName}`;
}

const INITIAL_UPDATE_ID = -1;
let globalNextUpdateId = 0;

/**
 * Panel (editor tab) with the visualization for a NeoJoin document.
 */
export default class VisualizationPanel {
    #document: vscode.TextDocument;
    webview: vscode.WebviewPanel;
    #pendingUpdateTimeout: NodeJS.Timeout | undefined = undefined;
    #lastUpdateId: number = INITIAL_UPDATE_ID;
    #options: PanelOptions;
    #client: VisualizationClient;
    #onDestroy: () => void;

    get mode() {
        return this.#options.mode;
    }

    constructor(
        webview: vscode.WebviewPanel,
        document: vscode.TextDocument,
        options: PanelOptions,
        client: VisualizationClient,
        onDestroy: () => void
    ) {
        this.#document = document;
        this.webview = webview;
        this.#options = options;
        this.#client = client;
        this.#onDestroy = onDestroy;

        webview.onDidDispose(() => {
            clearTimeout(this.#pendingUpdateTimeout);
            this.#onDestroy();
        });

        webview.webview.onDidReceiveMessage((message?: Record<string, any>) => {
            if (message?.command === "option") {
                (this.#options as any)[message.option] = message.value;
                this.update(true);
            }
        });

        webview.title = getPanelTitle(document);
        webview.webview.html = createPanelContent(document.uri, options);

        this.update(true);
    }

    /**
     * Update the visualization.
     * @param instant whether to update immediately or debounce the update
     */
    update(instant: boolean) {
        if (instant) {
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

    /**
     * Execute the visualization update (internal).
     */
    async #doUpdate() {
        if (this.#pendingUpdateTimeout) {
            clearTimeout(this.#pendingUpdateTimeout);
            this.#pendingUpdateTimeout = undefined;
        }
        const updateId = globalNextUpdateId++;

        const mode = this.#getModeForRequest();
        if (mode === null) {
            this.#sendToWebview(
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

        // handle response
        if (result.status === "rejected") {
            const message = getErrorMessage(result.reason);
            if (message === "No query found at cursor position.") {
                this.#sendToWebview(MessageType.Warning, message);
            } else if (message === "Timeout while rendering.") {
                this.#sendToWebview(
                    MessageType.Error,
                    "Timeout while rendering. Consider using the 'Selected' mode for large models. Also note that timeouts can leak rendering processes, which can degrade system performance."
                );
            } else {
                console.error(
                    "Error while trying to get visualization:",
                    result.reason
                );
                this.#sendToWebview(MessageType.Error, "Error: " + message);
            }
        } else {
            const type = result.value.message
                ? MessageType.Warning
                : MessageType.Ok;
            this.#sendToWebview(type, result.value.message, result.value.svg);
        }
    }

    /**
     * Send an update message to the webview.
     * @param type message type
     * @param message optional info message to show
     * @param svg optional visualization as SVG string to display
     */
    #sendToWebview(type: MessageType, message = "", svg?: string) {
        this.webview.webview.postMessage({ type, message, svg });
    }

    /**
     * Returns the mode for the visualization request.
     * @returns the mode for the request, or `null` if in `selected` mode and no selection is available
     */
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

    /**
     * Request visualization data from the server and wrap the result as a {@link PromiseSettledResult}.
     * @param mode
     * @returns
     */
    #requestVisualization(mode: Mode) {
        return promiseResult(
            this.#client.getTargetModelVisualization(this.#document, mode, {
                orthogonalArrows: this.#options.orthogonal,
                darkMode: isDarkThemeEnabled(),
            })
        );
    }

    /**
     * Focus the panel.
     */
    reveal() {
        this.webview.reveal();
    }

    /**
     * Close the panel and clean up resources.
     */
    close() {
        // triggers webview.onDidDispose to clean up timers etc.
        this.webview.dispose();
    }
}
