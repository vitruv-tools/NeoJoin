# opTGGs: Relational operators for Triple Graph Grammars

This project is a prototype implementation for transforming relational operators into TGGs.

## Theoretical introduction

We analyse the following SQL queries:
```sql
SELECT id AS name
FROM Station
JOIN Stop ON id;

SELECT name, color
FROM Line
WHERE name LIKE S%;
```
The first query selects all Stations that are also Stops and returns their id as name.
The second query selects all Lines that start with an "S" and returns their name and color.

We can see that those queries consist of a selection, a projection and an optional filter part:
```sql
SELECT id AS name   // projection
FROM Station        // selection
JOIN Stop ON id;    // selection

SELECT name, color  // projection
FROM Line           // selection
WHERE name LIKE S%; // filter
```
In classic relational algebra, the join operator is not part of the selection but is its own operator.
However, if we see a selection as the part that introduces all source tables from where we can project values into our 
new view, the join becomes part of the selection. Analogously, we see filters as operators that constrain the records
that appear in the projected view, but do not introduce new data sources. With this new conceptual understanding,
we can write these queries with these conceptual three parts:
```
σ(Station ⨝(id) Stop) π(id => name)
σ(Line) φ(name.startsWith("S")) π(name) π(color)
```
We select all Stations that can be joined with Stops and project their id to name in the first query.
For the second query, we select all Lines that nave a name that starts with "S" and project their name and color.

Relational algebra is concerned with relations (or tables) and links between them. In modeling, we are more concerned 
with objects and their relations. So how can we apply this relational queries to an object structure? Instead of tables
Station, Stop and Line, we have classes with the same names. Records then become object instances of these classes.
We can then write the same queries to get the same results:
```
σ(Station ⨝(id) Stop) π(id => name)
σ(Line) φ(name.startsWith("S")) π(name) π(color)
```
This creates two anonymous classes and transforms matching object instances to them. However, we usually want to give
those classes a name. Therefore, we specify the projected class explicitly (in SQL, we would create stored view):
```
σ(Station ⨝(id) Stop => Station) π(id => name)
σ(Line => Line) φ(name.startsWith("S")) π(name) π(color)
```

While records in tables only have attributes, objects can have references to other objects too (in RDBS, foreign keys 
can be used to emulate references). Modeling theory makes a distinction between referenced objets and contained objects.
The former can exist on their own, while the latter require the existence of the containing object. We found that this
distinct behavior has direct practical consequences in objects transformations with TGGs. Therefore, we use different
operators for references (λ) and containments (κ). While technically these are projections as well, keep them separated from 
attribute projections.

A reference (or containment; we mean both in this section) is actually a selection on referenced objects. If we have a
Line that references multiple Stations with reference named "stations", we actually have a subselection like this:
```
λ(Line-[stations]->Station)
```
Note that we want to transform the *reference* here and not the objects. Therefore, we do not know how the `Station` in
this example is transformed. We denote this with a `?`. A full list of queries looks like this:
```
σ(Line => Line) λ(Line-[stations]->Station => Line-[places]->?)
σ(Station => Landmark)
```
In the first query, we select all Lines and transform their `stations` references to `places` references to objects
of an unknown class. The second query selects all Stations and transforms them to Landmarks. When building TGG from 
queries, we need to resolve this unknown class. This is done in *resolved queries*. These are simply queries where 
this ambiguity is resolved by looking at other queries. In this case, we can resolve the first query by taking the
second query into account. Note that the resolved reference operator uses an uppercase lambda to distinguish it from an
unresolved reference operator:
```
σ(Line => Line) Λ(Line-[stations]->Station => Line-[places]->Landmark)
σ(Station => Landmark)
```
We can follow and flatten multiple references like in this example:
```
σ(Line) λ(Line-[stations]->Station-[platforms]->Platform => Line-[platforms]->?)
σ(Platform)
```
This collects all Platforms that are connected to a Line through a Station and projects them to a simple reference.
If we have, for example, two stations on our line, each with two platforms, we would project them to a list of four 
platforms on our projected line. Note that we did not need to select the Stations in a separate query.

Why did we need to specify `Line` in the reference operator? If we have selected multiple classes, e.g. with a join,
we need to know which of these classes have this reference:
```
σ(Line ⨝(color) Color => Line) λ(Line-[stations]->Station => Line-[stations]->?)
```
Alternatively, this could have happened the other way around. Therefore, we need this information on the projection 
side as well:
```
σ(Line => Line ⨝(color) Color) λ(Line-[stations]->Station => Line-[stations]->?)
```

To recap, we found that a query consists of a selection, some projections and some optional filters. A selection has
a source part that denotes what is selected, and a target part that denotes how it is transformed. We can use joins and
reference patterns on both sides to either select complex connected objects or to create them. As projections, we have
simple attribute projections, as well as reference and containment projections. The latter two consist of subselections
and optional filters.

## Patterns

In an object-oriented world, we are not only interested in objects, but also in relations between objects. There are two
major types of relations: explicit references between objects, and implicit relations due to object properties. The former
is expressed through the reference operator `-[ref]->`, whereas the latter is expressed as a join `⨝(pred)` with a 
predicate `pred`. Related objects may be transformed together. In that case, they form a strongly connected object pattern,
which we express with the `Pattern` class. A pattern consists of at least one object. Patterns always form a linear chain.
E.g. it is not possible to have a strongly connected pattern where one object references two other objects. We imagine
patterns as reference/join chains that grow from top to bottom. Therefore, the first object in a pattern is the top object
and the last object is the bottom object. For example, in the pattern `A-[b]->B ⨝(id) C`, A is the top object and C is
the bottom object. We will need these two objects later. Due to the linearity, we will always have one top and one bottom 
object (theoretically, it may be possible that patterns only need to be lattices or half-lattices).

We can build patterns easily with a builder/fluent interface:
```java
var pattern = Pattern.from(fqn("A")).ref(fqn("B"), "b").join(fqn("C"), "id");
```
