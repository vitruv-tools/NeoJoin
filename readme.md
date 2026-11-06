# NeoJoin

[![GitHub Action CI](https://github.com/vitruv-tools/NeoJoin/actions/workflows/ci.yml/badge.svg)](https://github.com/vitruv-tools/NeoJoin/actions/workflows/ci.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=vitruv-tools_NeoJoin&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=vitruv-tools_NeoJoin)
[![Issues](https://img.shields.io/github/issues/vitruv-tools/NeoJoin.svg)](https://github.com/vitruv-tools/NeoJoin/issues)
[![License](https://img.shields.io/github/license/vitruv-tools/NeoJoin.svg)](https://raw.githubusercontent.com/vitruv-tools/NeoJoin/main/LICENSE)

*NeoJoin* is a declarative query language for view-based model-driven software development. It allows to easily create views based on one or more source models using a declarative SQL-like syntax. It supports two backends for the model-view transformation: an EMF-based backend and a backend based on triple graph grammars (TGGs) that supports bidirectional and incremental transformations between models and views. Further tools that build on the language, such as an editor extension, and the TGG backend live in separate repositories (see [Related Repositories](#related-repositories)).

### Syntax Example

```
export reviewedrestaurants to "http://vitruv.tools/models/reviewedrestaurant"

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

#### Abstract Classes and Inheritance

```
abstract Item {
    name: EString
}

from extends Food
create Dish extends Item {
    name = name
}
```

NeoJoin supports the usage and definition of inheritance in meta-models. Defining inheritance in the view type covers multiple aspects: `abstract` declares an abstract class without instances and without root reference, while `extends` makes a class inherit from other classes (abstract or concrete) of the view type.

Abstract classes declare features, for which extending classes must define the transformation. An override takes the modifiers and the type of the overridden feature. An explicitly written type must be equal to the type of the overridden feature. The type of an attribute expression must be equal to the type of the overridden feature, while the type of a reference expression can be the same class or a sub class. A class can extend several classes. A feature that is inherited from several of them must come from the same declaration (a common super class), because Ecore does not allow two features with the same name in a class.

`from` and `join` select only the direct instances of the specified source class, while `from extends` and `join extends` also select the instances of its sub classes. As an abstract class has no direct instances, NeoJoin warns about `from` and `join` of an abstract class without `extends`. Subqueries select the instances of their source class and of its sub classes. A query without body copies all features of its source class, including inherited ones:

```
from Restaurant r
join extends Food f
    on r.sells.contains(f)
create Offer {
    restaurant := r.name
    dish := f.name
}

from extends Food
create Dish extends Item
```

A feature with an explicit class type, e.g. `offers: Item [*] := r.sells`, needs a query that selects the instances the expression returns and whose target class is the explicit type or a sub class of it (here `from extends Food create Dish extends Item`). A `from Food` query selects only direct instances, so values of sub classes of `Food` need a query of their own or a `from extends` query. If several queries select exactly the class of the value, the assignment is ambiguous and NeoJoin reports an error.

Classes that NeoJoin creates implicitly for referenced source classes (e.g. the target of a copied reference) select only the direct instances of their source class and are abstract if the source class is abstract. A reference to an instance that no class selects fails during the transformation, so the sub classes of a referenced class must be selected by a query or be referenced themselves. An abstract class without source that has the name of a referenced source class is used instead of an implicit class. An implicit class keeps the inheritance of its source class: it extends the class of its nearest source super class that is part of the view, as long as it has an equivalent feature (same name, type and modifiers) for each feature of it. Otherwise NeoJoin warns that the class is not linked. If several classes qualify, the query is ambiguous, and you need an explicit query with `extends`.

*Note that queries do not inherit from each other:* `extends` only specifies inheritance between the classes of the resulting view type. The behavior of a query (source, join, filter, grouping, feature expressions) cannot be inherited, so a class that extends another class still has to define how each of its features is computed. Subqueries cannot declare `extends`, so their classes have no super classes (they can be super classes of other classes).

### Features

* *Generation of the view type* (view meta-model) based on a query
* *Conformance check* of the generated view type against a reference metamodel, reported directly in the editor
* *Transformation* of models to derive an instance of a view based on a query
* *Bidirectional and incremental transformations* using the TGG backend
* Queries can *select*, *join*, *filter* and *group* classes from the source models
* *Abstract classes* and *inheritance* between classes of the view (`abstract`, `extends`), and selecting instances of sub classes (`from extends`)
* Features in the view can be *copied* from the source models, *renamed* or *calculated* based on a custom expression
* Query conditions and feature definition using [Xtend expressions](https://eclipse.dev/Xtext/xtend/documentation/203_xtend_expressions.html)
* Parameterizable queries with typed parameters passed at transformation time
* Language server (LSP) for syntax highlighting, code completion, and live visualization of the resulting view type
* Input and output meta-models as `.ecore` and instance-models as `.xmi` files
* Source meta-models may reference classes of the Ecore meta-model (e.g. `EObject`) without having Ecore on the meta-model path or importing it

### Keywords

Keywords of the query language are reserved everywhere in a query, including inside expressions, in addition to the
keywords of [Xbase](https://eclipse.dev/Xtext/xtend/documentation/203_xtend_expressions.html). The
[grammar](frontend/language/src/main/xtext/tools/vitruv/neojoin/NeoJoin.xtext) defines the current set. To use a
keyword as the name of a feature, an alias or a method, escape it with `^`, e.g. Xtend's `join` extension method:

```
from Food f
group by true
create Foods {
    name := f.map[ it.name ].sort.^join("+")
}
```

## Related Repositories

| Repository                                                                     | Content                                                                                                                                                                                                                                                                                  |
|--------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [NeoJoin-for-VS-Code](https://github.com/vitruv-tools/NeoJoin-for-VS-Code)     | [VSCode](https://code.visualstudio.com/) extension for syntax highlighting, code completion, and live visualization of the resulting view type. It starts the language server built in this repository.                                                                                  |
| [NeoJoin-TGG-Backend](https://github.com/vitruv-tools/NeoJoin-TGG-Backend)     | Backend based on triple graph grammars (TGGs) for bidirectional and incremental transformations between models and views, using [eMoflon::Neo](https://github.com/eMoflon/emoflon-neo) and [Neo4j](https://neo4j.com/). It is used through its Java API and is not available in the CLI. |
| [NeoJoin-for-Vitruvius](https://github.com/vitruv-tools/NeoJoin-for-Vitruvius) | Integration of NeoJoin into [Vitruvius](https://github.com/vitruv-tools/Vitruv): generates Java code for the view types of NeoJoin queries and supports their use in change propagation specifications.                                                                                  |

## Usage

NeoJoin comes with two frontends and two backends. The first frontend is the language server (LSP), which provides IDE support and view type visualization to editor extensions. The second frontend is the CLI, which provides commands for generating the view type, as well as executing the model-level transformations, currently only with our unidirectional, state-based EMF backend. The other backend offers bidirectional, incremental model-level transformations by generating triple graph grammars (TGGs).

The CLI executable and the language server can be downloaded from the build results of the last [successful nightly builds](https://github.com/vitruv-tools/NeoJoin/actions/workflows/ci.yml?query=branch%3Amain+event%3Aschedule+is%3Asuccess).

### Requirements

* JDK 21+ *(the language server runs on JDK 17+)*
* [Graphviz](https://plantuml.com/graphviz-dot) *(for the view type visualization in the language server)*
* The PlantUML jar (`plantuml-<version>.jar` from the [PlantUML releases](https://github.com/plantuml/plantuml/releases)) *(for the view type visualization in the language server, see the note below)*

**Note for projects that depend on the IDE module:** the shaded jar of the language server (`tools.vitruv.neojoin.frontend.ide`) does not contain PlantUML. Without it, the language server works, but visualization requests fail with the error "PlantUML was not found on the class path of the language server". Editor integrations that want to support the visualization have to ask their users for a PlantUML jar and start the language server with it on the class path. The version the language server is built against is managed in the root `pom.xml`.

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
  -p, --parameters=PARAMS[,PARAMS...]
                           Query parameters as comma-separated name=value
                             pairs. Supported Scalar Types: EString, EInt,
                             EDouble, EBoolean, ELong and EFloat. Strings with
                             spaces have to be quoted. For EClass/EList
                             parameters the value is a path to an XMI file.
                             Example: -p featureName=Navigation,
                             activeFeatures=config.xmi
  -t, --transform=OUTPUT   Transform the input models based on the query and
                             write the result to the given output file or
                             directory.

Model Path
  A semicolon separated list of paths to search for models
  used in the options --meta-model-path and --instance-model-path.
  Examples:
  - Linux: /path/to/directory;/path/to/file.ecore
  - Windows: C:\path\to\directory;C:/path/to/file.ecore
```

*Note: `QUERY` and `OUTPUT`s need to be specified with regular paths, e.g., `/x/y/z` or `./x/y`.*

### Conformance Validation

A view type often has to fit a meta-model that exists outside the query, for example because a tool that consumes the view was written against it. A query can name such a *reference meta-model* with the optional `conforms to` clause:

```
export food-view to "https://food.example.com/"
conforms to "https://food-reference.example.com/"

from Pizza create Food { type := "pizza" }
```

Whenever the clause is present, NeoJoin generates the view type, compares it against the reference with [EMF Compare](https://eclipse.dev/emfcompare/), and evaluates every structural difference against a catalogue of rules. Each rule decides whether the difference it matches leaves the conformance relation intact, breaks it in a way the query can repair, or breaks it in a way that requires a change to the reference meta-model. Violations are reported on the `conforms to` clause, so they appear in the editor while the query is written. Without the clause, nothing changes and no check runs.

Which differences count as a violation depends on the strictness:

| Strictness    | Requirement                                                                                                                       |
|---------------|-----------------------------------------------------------------------------------------------------------------------------------|
| `SUBTYPE`     | The view type has to be usable wherever the reference is expected. Additional structure in the view type is accepted. *(default)* |
| `EXACT_MATCH` | The view type has to reproduce the reference exactly, so every difference that one of the rules covers is reported.               |

Not every difference is covered by a rule. Moves and changes to the package name, `nsURI` or `nsPrefix` are always ignored, and differences that no rule matches (for example changes to `ordered`, `derived` or `changeable`, annotations, or a renamed element, which EMF Compare reports as an addition plus a deletion) produce no violation under either strictness.

The strictness is selected per view type with the optional `strict` keyword. A plain `conforms to` clause uses `SUBTYPE`, while `strict conforms to` uses `EXACT_MATCH`:

```
export food-view to "https://food.example.com/"
strict conforms to "https://food-reference.example.com/"

from Pizza create Food { type := "pizza" }
```

The rules and their categories are documented in the [conformance validator readme](/conformancevalidator/readme.md).

## Repository Structure

| **Module** / Package     | Description                                                                        |
|--------------------------|------------------------------------------------------------------------------------|
| docs                     | Project documentation                                                              |
| **backend-emf**          | EMF-based transformation engine to derive instances of the view based on the query |
| **expression-parser**    | Parser for the reference expressions of features                                   |
| **conformancevalidator** | Rule-based check whether a generated view type conforms to a reference meta-model  |
| frontend/**cli**         | CLI interface to execute meta-model generation and instance model transformation   |
| frontend/**ide**         | IDE support via language server protocol (LSP)                                     |
| frontend/**language**    | Main language implementation and meta-model generator                              |
| **model**                | Abstract query representation (AQR)                                                |

## Build

### Requirements

* JDK 21+

### Maven Projects

```sh
# either: re-generate and compile the code
./mvnw clean compile
# or: skip the generation, compile the code and run the tests
./mvnw test -P skip-code-generation
# or: package everything to JAR files
./mvnw verify
```

We use [SonarQube](https://sonarcloud.io/project/overview?id=vitruv-tools_NeoJoin) to analyze our Maven projects.

### Notes

* Xtext heavily uses generated classes which means that opening this repository in a Java IDE after cloning will look like a christmas tree. Run `maven compile` to generate all missing classes. To improve compile times afterwards you can skip re-generation of classes by activating the maven profile `skip-code-generation`.

## Used Technology

* [EMF](https://eclipse.dev/emf/) as modeling framework
* [Xtext](https://eclipse.dev/Xtext/) as DSL framework
* [Xbase](https://eclipse.dev/Xtext/documentation/305_xbase.html) as embedded expression language
