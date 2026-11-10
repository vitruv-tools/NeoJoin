# Jar Metadata and Licenses

Every jar contains the license of this project as `META-INF/LICENSE` and the manifest entry `Automatic-Module-Name` (the artifact ID, so users get a stable module name on the module path; the property `automatic.module.name` sets it for modules whose artifact ID contains a hyphen, because a hyphen is not valid in a module name).

The CLI (`frontend/cli`) and the language server (`frontend/ide`) are fat jars: the shade plugin bundles all dependencies into them. Besides the project license they contain what the licenses of the bundled dependencies ask for:

* `META-INF/NOTICE`: the NOTICE files of all dependencies, merged.
* `META-INF/THIRD-PARTY.txt`: every bundled dependency with its Maven coordinates and its license.
* `META-INF/licenses/<dependency>/`: the license, notice and about files that the jar of the dependency contains.
* `META-INF/licenses/*.txt`: the license texts in `third-party-licenses/` (repository root), for dependencies whose jar contains none (each file names its source; `third-party-licenses/README.txt` explains the layout).

The license files of the dependencies themselves are not copied to the top level of the jar (`META-INF/LICENSE*`, `module-info.class` and similar are excluded in the shade configuration), because the shade plugin would keep one arbitrary copy of each.

If you add a dependency to one of the two modules:

* The build fails when its POM has no license. Add it to `third-party-licenses.properties` (`groupId--artifactId--version=license`).
* If the jar of the dependency contains no license file (see `META-INF/licenses/` in the built jar), add the license text to `third-party-licenses/` (copy it from the project, not from memory, and name the source in the file) and list the dependency in `third-party-licenses/README.txt`.
* Check the shade overlap warnings of the build: they are shown, new ones should be resolved with a filter or a transformer.
