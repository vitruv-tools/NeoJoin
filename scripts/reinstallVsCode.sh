
set -o errexit

error() {
    local exit_after_print=false
    if [[ "$1" == "-e" ]]; then
        exit_after_print=true
        shift
    fi
    echo -e "\e[0;31m$1\e[0m" >&2
    if $exit_after_print; then
        exit 1
    fi
}

# This scripts will reinstall the vscode plugin from the local repository
# It assumes it is executed in <path to your neojoin project directory>/vscode-plugin
#
# There is also a vscode launch script under vscode-plugin/.vscode/launch.json (also see readme.md).
# This one is for people who prefer working from the terminal.

if [[ "$(basename "$PWD")" != "vscode-plugin" ]]; then
    # try to navigate to right path if inside of NeoJoin project directory or in the top level scripting directory
    cd "vscode-plugin" || cd cd "../vscode-plugin" || error -e "you need to execute this script inside the vscode-plugin directory"
    echo "changed into vscode-plugin"
fi

# NOTE: paths in the following are relative to the vscode-plugin directory which is expected to be placed sided by side with the dir for the language
PATH_OF_LSP="${PATH_OF_LSP:-tools.vitruv.neojoin.frontend.ide.jar}"

PATH_OF_LANG="${PATH_OF_LANG:-../lang}"

PATH_WHERE_TO_GET_LSP="${PATH_OF_LANG}/frontend/ide/target/tools.vitruv.neojoin.frontend.ide.jar"

PATH_MVNW="${PATH_OF_LANG}/mvnw"
[[ -f "$PATH_MVNW" ]] || error -e "PATH_MVNW is not a file: $PATH_MVNW"

MVN_CMD="${MVN_CMD:-$(realpath $PATH_MVNW || error -e "$PATH_MVNW no such file or directory") package -DskipTests}"

if [[ ! -f "$PATH_OF_LSP" ]]; then
    echo "lsp not found -> obtain lsp"
    if [[ ! -f "$PATH_WHERE_TO_GET_LSP" ]]; then
        {
            cd "$PATH_OF_LANG"
            echo "building lsp"
            eval "$MVN_CMD"
        } || error -e "failed to build lsp"

        cd -
    fi

    cp "$PATH_WHERE_TO_GET_LSP" . && echo "copied lsp" ||
        error -e "failed to obtain lsp"
else
    echo "lsp already exists"
fi

echo "building extension"
{
    npm install
    yes | npx @vscode/vsce package
} || error -e "failed to build package"

echo "uninstalling old extension"
if code --list-extensions | grep -Fq "vitruv-tools.neojoin"; then
    code --uninstall-extension vitruv-tools.neojoin && echo "uninstalled extension" || error -e "failed to uninstall extension"
fi

close_code() {
    # currently this does only support the oss version since it is the one I use. If any one uses a different version just add it to the for loop
    for process_name in "code-oss"; do
        killall "$process_name" && echo "closed vscode" && return
    done

    echo "ATTENTION: Did not close vs code."
    echo "Either it was not running or the it does not work on your system because the process has a different name. In the last case you need to close it manually or add the name to the script."
}

echo "close vs code if open"
close_code

echo "installing extension"
code --install-extension neojoin-1.0.0.vsix && echo "installed extension" || error -e "failed to install extension"

code
