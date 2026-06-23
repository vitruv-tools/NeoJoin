
# This scripts will reinstall the vscode plugin from the local repository
# It assumes it is executed in <path to your neojoin project directory>/vscode-plugin
#
# There is also a vscode launch script under vscode-plugin/.vscode/launch.json (also see readme.md).
# This one is for people who prefer working from the terminal.

# NOTE: paths in the following are relative to the vscode-plugin directory which is expected to be placed sided by side with the dir for the language
PATH_OF_LSP="${PATH_OF_LSP:-tools.vitruv.neojoin.frontend.ide.jar}"
PATH_OF_LANG="${PATH_OF_LANG:-../lang}"
PATH_WHERE_TO_GET_LSP="${PATH_OF_LANG}/frontend/ide/target/tools.vitruv.neojoin.frontend.ide.jar"
PATH_MVNW="${PATH_OF_LANG}/mvnw"
MVN_CMD="${MVN_CMD:-$(realpath $PATH_MVNW) package -DskipTests}"

error() {
    echo -e "\e[0;31m$1\e[0m" >&2
}

[[ "$(basename "$(dirname "$(realpath $0)")")" == ".scripts" ]] && cd ..

if [[ "$(basename "$(dirname "$(realpath $0)")")" != "vscode-plugin" ]]; then
    cd "vscode-plugin" &>/dev/null || {
        error "you need to execute this script inside the vscode-plugin directory"
        exit 1
    }
fi

if [[ ! -f "$PATH_OF_LSP" ]]; then
    if [[ ! -f "$PATH_WHERE_TO_GET_LSP" ]]; then
        {
            cd "$PATH_OF_LANG"
            eval "$MVN_CMD"
        } || {
            error "failed to build lsp"
            exit 1
        }

        cd -
    fi

    cp "$PATH_WHERE_TO_GET_LSP" . || {
        error "failed to obtain lsp"
        exit 1
    }
fi


{
    npm install
    yes | npx @vscode/vsce package
} || {
    error "failed to build package"
    exit 1
}

if code --list-extensions | grep -Fq "vitruv-tools.neojoin"; then
    code --uninstall-extension vitruv-tools.neojoin && echo "uninstalled extension" || {
        error "failed to uninstall extension"
        exit 1
    }
fi

close_code() {
    # currently this does only support the oss version since it is the one I use. If any one uses a different version just add it to the for loop
    for process_name in "code-oss"; do
        killall "$process_name" && echo "closed vscode" && return
    done

    echo "ATTENTION: Did not close vs code."
    echo "Either it was not running or the it does not work on your system because the process has a different name. In the last case you need to close it manually or add the name to the script."
}

close_code

code --install-extension neojoin-1.0.0.vsix && echo "installed extension" || {
    error "failed to install extension"
    exit 1
}

code
