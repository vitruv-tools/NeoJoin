import * as vscode from "vscode";
import { expandVariables } from "./expand";

class Config {
    #get<T>(key: string, defaultValue: T): T {
        return vscode.workspace.getConfiguration("neojoin").get<T>(key, defaultValue);
    }

    /**
     * Path to search for available meta models that can be imported in queries.
     */
    get metaModelSearchPath() {
        return expandVariables(this.#get("metaModelSearchPath", "${workspaceFolderUri}"));
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
