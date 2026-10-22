# Conformance Validator

Checks whether a view type conforms to a reference meta-model. A view type conforms to a reference if every valid instance of the view type is also a valid instance of the reference, so that anything written against the reference keeps working when it is handed a view.

The module depends on EMF and EMF Compare. It compares two `EPackage`s and does not need a NeoJoin query as input, so it can be used on its own. How NeoJoin uses it through the `conforms to` clause is described in the [project readme](/readme.md#conformance-validation).

## Structure

The check runs in two phases.

**Phase I** compares the two metamodels with EMF Compare and returns a list of `Diff`s. The view type is always the left side and the reference the right side. An `ADD` therefore means that an element exists in the view type but not in the reference, and a `DELETE` means the reverse. The default comparison and matching engines are used, so Phase I reports every structural difference without filters.

**Phase II** evaluates every difference against every rule that the selected strictness allows. A rule is written against one specific kind of difference and either applies to a given difference or does not, so it never reports more than one violation per difference.

## Strictness

Every rule carries one of three categories, and the strictness decides which of them are evaluated:

| Category                  | Meaning                                                                        | `SUBTYPE` | `EXACT_MATCH` |
|---------------------------|--------------------------------------------------------------------------------|-----------|---------------|
| `NON_BREAKING`            | The difference leaves the conformance relation intact                          | ignored   | reported      |
| `BREAKING_RESOLVABLE`     | The relation is broken, and the query can be rewritten to repair it            | reported  | reported      |
| `BREAKING_NOT_RESOLVABLE` | The relation is broken, and only a change to the reference metamodel repairs it | reported  | reported      |

Rules of a category the chosen strictness does not allow are never evaluated.

## Rules

Rule 0 and Rule 1 have no rule class. They state that a difference is conforming under every strictness and are listed here because they appear in the difference list.

| Rule    | Difference                | Condition                                                      | Category                  |
|---------|---------------------------|-----------------------------------------------------------------|---------------------------|
| Rule 0  | `MOVE`                    | elements of a multi-valued feature appear in a different order  | ignored                   |
| Rule 1  | `CHANGE` on package data  | `name`, `nsURI` or `nsPrefix` of the package differ              | ignored                   |
| Rule 2  | `ADD` of feature          | the view type declares a feature the reference does not          | `NON_BREAKING`            |
| Rule 3  | `DELETE` of feature       | the feature is mandatory in the reference                        | `BREAKING_RESOLVABLE`     |
| Rule 4  | `DELETE` of feature       | the feature is optional in the reference                         | `NON_BREAKING`            |
| Rule 5  | `CHANGE` on `lowerBound`  | the reference requires more values than the view type            | `BREAKING_RESOLVABLE`     |
| Rule 6  | `CHANGE` on `lowerBound`  | the view type requires at least as many values as the reference  | `NON_BREAKING`            |
| Rule 7  | `CHANGE` on `upperBound`  | the view type allows more values than the reference              | `BREAKING_RESOLVABLE`     |
| Rule 8  | `CHANGE` on `upperBound`  | the view type allows the same number of values or fewer          | `NON_BREAKING`            |
| Rule 9  | `CHANGE` on `eType`       | the type of the view type is not a subtype of the reference type | `BREAKING_RESOLVABLE`     |
| Rule 10 | `CHANGE` on `eType`       | the type of the view type is a subtype of the reference type     | `NON_BREAKING`            |
| Rule 11 | `DELETE` of class         | a feature of the reference requires the class                    | `BREAKING_RESOLVABLE`     |
| Rule 12 | `DELETE` of class         | no feature of the reference requires the class                   | `NON_BREAKING`            |
| Rule 13 | `DELETE` of supertype     | the supertype declares a mandatory feature, even if the class declares it itself | `BREAKING_NOT_RESOLVABLE` |
| Rule 14 | `ADD` of supertype        | the view type class extends a supertype the reference does not   | `NON_BREAKING`            |
| Rule 15 | `DELETE` of supertype     | the supertype declares no mandatory feature                      | `NON_BREAKING`            |
| Rule 16 | `DELETE` of enum          | a feature of the reference requires the enumeration              | `BREAKING_RESOLVABLE`     |
| Rule 17 | `DELETE` of enum          | no feature of the reference requires the enumeration             | `NON_BREAKING`            |
| Rule 18 | `ADD` of enum literal     | the view type declares a literal the reference does not          | `BREAKING_NOT_RESOLVABLE` |
| Rule 19 | `DELETE` of enum literal  | the reference declares a literal the view type does not          | `NON_BREAKING`            |
| Rule 20 | `CHANGE` on `unique`      | the reference requires unique values, the view type does not     | `BREAKING_RESOLVABLE`     |
| Rule 21 | `CHANGE` on `unique`      | the view type requires unique values, the reference does not     | `NON_BREAKING`            |
| Rule 22 | `CHANGE` on `containment` | the containment differs, in either direction                     | `BREAKING_RESOLVABLE`     |
| Rule 23 | `CHANGE` on `abstract`    | the reference declares the class abstract, the view type does not | `BREAKING_NOT_RESOLVABLE` |
| Rule 24 | `CHANGE` on `abstract`    | the view type declares the class abstract, the reference does not | `NON_BREAKING`            |
| Rule 25 | `CHANGE` on `eOpposite`   | the reference declares an opposite the view type does not         | `BREAKING_NOT_RESOLVABLE` |
| Rule 26 | `ADD` of package          | the view type declares a subpackage the reference does not       | `NON_BREAKING`            |
| Rule 27 | `ADD` of class            | the view type declares a class the reference does not            | `NON_BREAKING`            |
| Rule 28 | `ADD` of enum             | the view type declares an enumeration the reference does not     | `NON_BREAKING`            |
| Rule 29 | `DELETE` of package       | the subpackage contains a classifier the reference requires      | `BREAKING_NOT_RESOLVABLE` |
| Rule 30 | `DELETE` of package       | nothing outside the subpackage requires its classifiers          | `NON_BREAKING`            |

Five rules are not resolvable because NeoJoin has no way to express the element in a query: supertypes (Rule 13), the literals of an enum (Rule 18), abstract classes (Rule 23), opposite references (Rule 25) and subpackages (Rule 29). If the language is extended in those ways, the rules become resolvable.

Rule 13 does not check whether the view type class declares the mandatory features of the deleted supertype itself (flattened inheritance), so it can report a violation that the view type would not actually have.

Four rules cannot fire for a view type that NeoJoin generates, although they work on any pair of packages: Rule 10 (two types count as compatible only if they have equal names, which produces no difference, or if one is a supertype of the other, and a generated class has no supertypes, so a changed type is always reported by Rule 9), Rule 14 (a generated class has no supertypes), Rule 24 (a generated class is never abstract) and Rule 26 (a generated package has no subpackages).

Rules that react to an addition or a deletion only apply when the containing element exists on both sides. A classifier that is added or deleted as a whole is therefore reported once by its own rule, and not once more for each of its features or literals, which EMF Compare reports as separate differences.

## Structure

| Class                       | Description                                                                   |
|-----------------------------|--------------------------------------------------------------------------------|
| `ConformanceValidator`      | Entry point, runs both phases and holds the list of all rules                  |
| `Violation`                 | One violation: rule identifier, message, category and the underlying `Diff`    |
| `Category`                  | The three categories a rule can have                                          |
| `rules/ConformanceRule`     | Base class of every rule, with identifier, category and `check(Diff)`          |
| `rules/Strictness`          | The requirement the view type has to satisfy, selects the categories to evaluate |
| `rules/RuleUtils`           | Conditions and message helpers that more than one rule needs                   |

The rule classes are in three subpackages named after the categories: `rules/nonbreaking`, `rules/breakingresolvable` and `rules/breakingnotresolvable`. Within a pair of rules that react to the same kind of difference, the class that reports a violation has the plain name, for example `FeatureDeletedRule`, and the conforming counterpart in `nonbreaking` carries the prefix `Safe`, for example `SafeFeatureDeletedRule`.

## Adding a rule

1. Write a class in the subpackage of its category that extends `ConformanceRule`, passes its identifier and category to `super(...)` and implements `check(Diff)`.
2. Add an instance of it to `ALL_RULES` in `ConformanceValidator`.