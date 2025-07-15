import * as vscode from "vscode";
import { expandVariables } from "./expand";

class Config {
	#get<T>(key: string, defaultValue: T): T {
		return vscode.workspace.getConfiguration("neojoin").get<T>(key, defaultValue);
	}

	get metaModelSearchPath() {
		return expandVariables(this.#get("metaModelSearchPath", "${workspaceFolderUri}"));
	}

	get debug() {
		return this.#get("debug", false);
	}
}

const instance = new Config();
export default instance;
