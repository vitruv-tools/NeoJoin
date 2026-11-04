import * as vscode from "vscode";
import { expandVariables } from "./expand";

class Config {
    #get<T>(key: string, defaultValue: T): T {
        return vscode.workspace
            .getConfiguration("neojoin")
            .get<T>(key, defaultValue);
    }

    /**
     * Path to search for available meta models that can be imported in queries.
     */
    get metaModelSearchPath() {
        return expandVariables(
            this.#get("metaModelSearchPath", "${workspaceFolder}")
        );
    }

    /**
     * Names of directories that the server does not search for queries.
     */
    get excludedDirectories() {
        return this.#get<string[]>("excludedDirectories", [
            "target",
            "bin",
            "build",
            "node_modules",
            ".git",
        ]);
    }

    /**
     * Whether debug mode is enabled.
     */
    get debug() {
        return this.#get("debug", false);
    }
}

const instance = new Config();
export default instance;
