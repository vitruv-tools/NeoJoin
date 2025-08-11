# NeoJoin

*NeoJoin* is a declarative query language for view-based model-driven software development. It allows to easily create views based on one or more source models using a declarative SQL-like syntax. It supports multiple backends for the model-view transformation, one of which is based on triple graph grammars (TGGs) and supports bidirectional and incremental transformations between models and views.

*Note: The frontend is currently not connected to the TGG backend, so an automatic transformation of the queries in NeoJoin syntax is only possible with the EMF backend. We plan to add support for this in the future.*

### Syntax Example

```
export as reviewedrestaurants to "http://vitruv.tools/models/reviewedrestaurant"

import "http://vitruv.tools/models/restaurant"
import "http://vitruv.tools/models/reviewpage"

from Restaurant rest
join ReviewPage rev
    using name
where rev.reviews.length > 3
create ReviewedRestaurant {
    rest.name
    offeredDishes = rest.sells create Dish {
        it.name
        it.price
    }
    avgRating := rev.reviews.avg[ it.rating ]
    numReviews := rev.reviews.length
}
```

### Features

* *Generation of the view type* (view meta-model) based on a query
* *Transformation* of models to derive an instance of a view based on a query
* *Bidirectional and incremental transformations* using the TGG backend
* Queries can *select*, *join*, *filter* and *group* classes from the source models
* Features in the view can be *copied* from the source models, *renamed* or *calculated* based on a custom expression
* Query conditions and feature definition using [Xtend expressions](https://eclipse.dev/Xtext/xtend/documentation/203_xtend_expressions.html)
* [VSCode](https://code.visualstudio.com/) plugin for syntax highlighting, code completion, and live visualization of the resulting view type
* Input and output meta-models as `.ecore` and instance-models as `.xmi` files

## Structure

### Project
| Directory     | Content                                                      |
|---------------|--------------------------------------------------------------|
| docs          | Project documentation                                        |
| lang          | Prototype implementation *(see below)*                       |
| scripts       | Helper scripts for the project                               |
| scripts/dump  | Script for exporting models from a Neo4j graph database      |
| vscode-plugin | VSCode plugin for NeoJoin language support and visualization |

### Prototype Implementation
| **Module** / Package       | Description                                                                                             |
|----------------------------|---------------------------------------------------------------------------------------------------------|
| **backend-emf**            | EMF-based transformation engine to derive instances of the view based on the query                      |
| **backend-tgg**            | eMoflon::Neo-based transformation engine for bidirectional transformation between models and views      |
| backend-tgg/**driver**     | Auxiliary logic for generating eMoflon::Neo Eclipse projects                                            |
| backend-tgg/**operators**  | Primary operators for creating TGGs                                                                     |
| backend-tgg/**transpiler** | The core logic and operators for creating TGGs from operators                                           |
| frontend/**cli**           | CLI interface to execute meta-model generation and instance model transformation *(currently EMF-only)* |
| frontend/**ide**           | IDE support via language server protocol (LSP)                                                          |
| frontend/**language**      | Main language implementation and meta-model generator                                                   |
| **model**                  | Abstract query representation (AQR)                                                                     |

## Build

### Requirements

* JDK 21+
* Node.js 20 (+ NPM)

### Maven Projects

```sh
cd lang

# either: re-generate and compile the code
./mvnw clean compile
# or: skip the generation, compile the code and run the tests
./mvnw test -P skip-code-generation
# or: package everything to JAR files
./mvnw verify
```

### VSCode Plugin

* Open the `vscode-plugin` folder in VSCode (top-level in a workspace)
* Run `npm install` to install dependencies
* Go to `Run and Debug` > Select launch configuration `Launch Client` > Press `Start Debugging`
    * The language server needs a few seconds to start up, so code completion / analysis will not work right at the start.
    * The plugin requires that the `.jar` files have been generated and are at their default location.
    * If you cannot find the launch configuration `Launch Client`, ensure that you have opened VSCode with the `vscode-plugin` folder as your workspace.
* If you get the error message `Activating extension 'vitruv-tools.neojoin' failed: Cannot find module` check the build task for potential problems: Bottom Panel > Select tab `Terminal` > Select task `watch` (on the right)

### Notes

* Xtext heavily uses generated classes which means that opening this repository in a Java IDE after cloning will look like a christmas tree. Run `maven compile` to generate all missing classes. To improve compile times afterwards you can skip re-generation of classes by activating the maven profile `skip-workflow`.

## Usage

### Requirements

* JDK 21+
* [VSCode](https://code.visualstudio.com/) *(for the VSCode plugin)*
* [Neo4j](https://neo4j.com/) *(for the TGG backend)*
* [eMoflon::Neo](https://github.com/eMoflon/emoflon-neo) installed in an Eclipse instance *(for the TGG backend)*

### VSCode Plugin

Provides basic IDE support (syntax highlighting, code-completion, etc.) and a side-by-side live visualization of queries. Can be installed from the `.vsix` file via **Extensions** > 3-dot menu > **Install from VSIX...**.

#### Commands
* **Show Query Visualization** - Show a live visualization of the queries in the currently active text editor.
* **Restart Server** - Restart the language server after configuration changes.

#### Settings
* **Meta Model Search Path** - Semicolon separted list of file URLs to search for source meta models.
* **Debug** - Enable debug logging.

### Command Line Interface

```
Usage: neojoin [-hV] -m=MODEL-PATH [-g=OUTPUT] [-i=MODEL-PATH -t=OUTPUT] QUERY

      QUERY                Path to the query file.

  -h, --help               Show this help message and exit.
  -m, --meta-model-path=MODEL-PATH
                           Model path (see below) to find referenced
                             meta-models (.ecore).
  -V, --version            Print version information and exit.

Generate the meta-model:
  -g, --generate=OUTPUT    Generate the meta-model and write it to the given
                             output file or directory.

Transform the input models:
  -i, --instance-model-path=MODEL-PATH
                           Model path (see below) to find instance models (.xmi).
  -t, --transform=OUTPUT   Transform the input models based on the query and
                             write the result to the given output file or
                             directory.

Model Path
  A semicolon separated list of file URLs to search for models
  used in the options --meta-model-path and --instance-model-path.
  Examples:
  - Linux: file:///path/to/directory;file:///path/to/file.ecore
  - Windows: file:///C:/path/to/directory;file:///C:/path/to/file.ecore
```

*Note: `QUERY` and `OUTPUT`s need to be specified with regular paths, e.g., `/x/y/z` or `./x/y`.*

### Transformation of Operators to Triple Graph Grammars

1. Build view from operators directly: `var view = new View(); view.addQuery(...);`
2. Resolve interdependent queries (see documentation in `Query`): `var queries = view.resolveQueries()`
3. Transform queries to TGG rules: `var rules = query.toRules();`
4. Build triple grammar from TGG rules: `var grammar = new TripleGrammar(name, ..., rules);`
5. Optional: generate eMoflon::Neo project: `new Scaffolding(...).create(...);`

You can use the convenience function `API.generateProjectForView(...)` for steps 2 to 5.

### Running the Generated Model-View Transformations

*to do*

## Used Technology

* [EMF](https://eclipse.dev/emf/) as modeling framework
* [Xtext](https://eclipse.dev/Xtext/) as DSL framework
* [Xbase](https://eclipse.dev/Xtext/documentation/305_xbase.html) as embedded expression language
* [jte](https://jte.gg/) as templating engine
* [eMoflon::Neo](https://github.com/eMoflon/emoflon-neo) as TGG engine
* [Neo4j](https://neo4j.com/) as graph database
