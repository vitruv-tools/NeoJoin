# Dump Target eMSL model from Neo4j
This project allows dumping NeoCore models stored in a Neo4j database as textual eMSL models.
eMSL is a textual transformation and (meta)modelling language.
NeoCore allows to describe metamodels are Neo4j graphs. 
It follows the structure of [ECore](https://eclipse.dev/modeling/emf/).
NeoCore and eMSL are used by the graph transformation engine [eMoflon::Neo](https://github.com/eMoflon/emoflon-neo).

To dump the "Target" model in the database to the standard output run:

```sh
node dump
```

It queries the NeoCore metamodel to automatically identify attributes that are not specified by the metamodel.
Those attributes use a `~` instead of a `.` before the attribute name in the eMSL model.
It also checks the data type in the metamodel to identify enum values.

To dump synced models in the database, update the names in the call to `extractModels`.
