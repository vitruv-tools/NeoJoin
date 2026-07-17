
### DESCRIPTION ###
# This scripts will reinstall the vscode plugin from the local repository.
#
# There is also a vscode launch script under vscode-plugin/.vscode/launch.json (also see readme.md).
# This script offers an alternative to the vscode launch script mentioned above that may come in handy for developers using some other IDE then vscode.

## ASSUMPTIONS ##
#
## PLACE OF EXECUTION
# It assumes it is executed in <path to your neojoin project directory>/vscode-plugin.
# If you have the vscode-plugin directory and the lang directory placed next to each other in the file system, you are also able to run it from the lang or script directory.
#
## ACQUIRING THE JAR FOR THE LSP
# If the lsp jar file is already contained in the vscode-plugin directory it is used to build the plugin.
# If not it tries to find the lsp in the inside the lang directory.
# If it can find the lsp there it is copied over and used to build the plugin.
# If it can not find the lsp it will try to build it using the maven wrapper.
#
# For finding the lsp or the lang directory (where the java project for the lsp resides) it requires the lang directory and vscode-plugin directory to be placed next to each other.
# If you have another set up you can check the environment variables below and set your environment to fit your needs.
#
## CLOSING VSCODE
# In order apply the changes to the plugin vscode needs to be restarted.
# This script tries to automate that by killing the process.
# However, note that the name of the process can vary depending on your environment and application version (oss, codeium, original).
# It tries the following process names: "code-oss", "codeium", "vscodeium", "codeium", "code"
# If your process name is not among them, you either need to close the application manually or add it.
# (also see: function close_code())
#
## NAME OF VSCODE EXECUTABLE
# According to https://code.visualstudio.com/docs/configure/command-line the command for vscode is just "code" if that is not the case on your system you need an alias.


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
    # NOTE: The name of the process can vary depending on your environment and application (oss, codeium, original) version.
    for process_name in "code-oss" "codeium" "vscodeium" "codeium" "code"; do
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
