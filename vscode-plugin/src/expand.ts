import * as os from "os";
import * as path from "path";

import * as vscode from "vscode";

import { getErrorMessage } from "./utils";

const DEFAULT_VARS: Record<string, string> = {
    get userHome() {
        return os.homedir();
    },
    get workspaceFolder() {
        const folder = vscode.workspace.workspaceFolders?.[0];
        return folder ? folder.uri.fsPath : "";
    },
    get workspaceFolderBasename() {
        const folderPath = this.workspaceFolder;
        return folderPath ? path.basename(folderPath) : "";
    },
    get workspaceFolderUri() {
        const folder = vscode.workspace.workspaceFolders?.[0];
        return folder ? folder.uri.toString() : "";
    },
    get cwd() {
        return process.cwd();
    },
    get pathSeparator() {
        return path.sep;
    },
    get "/"() {
        return path.sep;
    },
};

function vars(arg: string): string | Error {
    if (Object.prototype.hasOwnProperty.call(DEFAULT_VARS, arg)) {
        return DEFAULT_VARS[arg];
    }

    return new Error(`Unknown variable ${arg}`);
}

function workspaceFolder(arg: string): string | Error {
    const folder = (vscode.workspace.workspaceFolders || []).find(
        (folder) => folder.name.toLocaleLowerCase() === arg.toLocaleLowerCase()
    );
    if (folder) {
        return folder.uri.fsPath;
    } else {
        return new Error(`No workspace folder with name ${arg}`);
    }
}

function env(arg: string): string {
    return process.env[arg] || "";
}

const PROVIDERS = new Map([
    ["", vars],
    ["workspaceFolder:", workspaceFolder],
    ["env:", env],
]);

/**
 * Resolve vscode variables in the input string. Implements only a subset of https://code.visualstudio.com/docs/reference/variables-reference
 * @param input input string
 * @returns input string with variables resolved
 */
export function expandVariables(input: string): string {
    return input.replace(/\$\{(?:(\w+:)?([^}]+))\}/g, (_, type, arg) => {
        const provide = PROVIDERS.get(type || "");
        if (!provide) {
            throw new Error(`Unknown variable replacement type: ${type}`);
        }

        const result = provide(arg);
        if (typeof result !== "string") {
            const typeStr = type ? `${type}:` : "";
            throw new Error(
                `Variable replacement failed (\${${typeStr}${arg}}): ${getErrorMessage(
                    result
                )}`
            );
        }

        return result;
    });
}
