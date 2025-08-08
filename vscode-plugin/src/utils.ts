import * as vscode from "vscode";

/**
 * Escape text for use in HTML including within <script> tags.
 * @param text text to escape
 * @returns escaped text
 */
export function escapeForHTML(text: string) {
    return text.replace(
        /[<>"'&]/g,
        (c) => `\\u${c.codePointAt(0)!.toString(16).padStart(4, "0")}`
    );
}

/**
 * Wrap the result of a promise (successful or not) as a {@link PromiseSettledResult}: `{ status: "fulfilled", value: T } | { status: "rejected", reason: any }}`.
 * @param promise promise to wrap
 * @returns a promise that resolves to a {@link PromiseSettledResult}
 */
export async function promiseResult<T>(
    promise: Promise<T>
): Promise<PromiseSettledResult<T>> {
    try {
        return { status: "fulfilled", value: await promise };
    } catch (error) {
        return { status: "rejected", reason: error };
    }
}

/**
 * Replace placeholders of the format `%key%` in {@link text} with values from {@link placeholders}.
 * @param text text with placeholders
 * @param placeholders map of placeholder keys to values
 * @returns text with placeholders replaced
 */
export function replacePlaceholder(
    text: string,
    placeholders: Record<string, string>
) {
    return text.replace(/%(\w+)%/g, (_, key) => {
        if (key in placeholders) {
            return placeholders[key];
        } else {
            throw new Error(`Unknown placeholder: ${key}`);
        }
    });
}

export function isDarkThemeEnabled() {
    return vscode.window.activeColorTheme.kind === vscode.ColorThemeKind.Dark;
}

export function getErrorMessage(error: any) {
    if (error instanceof Error) {
        return error.message;
    } else {
        return String(error);
    }
}

/**
 * Retrieves the cursor position in the given document.
 *
 * @param document the text document to get the position from
 * @returns the cursor position in the document
 * @throws if no editor is found for the document
 */
export function getSelectionInDocument(
    document: vscode.TextDocument
): vscode.Selection | null {
    const editor = vscode.window.visibleTextEditors.find(
        (editor) => editor.document === document
    );
    return editor?.selection ?? null;
}

/**
 * Returns a promise that resolves after the given duration.
 * @param duration duration in milliseconds
 * @returns a promise that resolves after the specified duration
 */
export function delay(duration: number): Promise<void> {
    return new Promise((resolve) => setTimeout(resolve, duration));
}
