# Conformance Validator – Review

## Was der Validator macht

Der Conformance Validator prüft, ob ein View Type, der aus einer NeoJoin-Query generiert wurde, noch zur Referenz passt. Dafür durchläuft er zwei Phasen: Phase I vergleicht View Type und Referenz strukturell mit EMF Compare und liefert eine Liste von Diffs. Phase II wertet diese Diffs gegen einen festen Regelsatz aus und entscheidet pro Diff, ob er konform ist oder nicht.

## ConformanceValidator

`ConformanceValidator` ist der Einstiegspunkt und führt beide Phasen aus. `compare(viewType, reference)` führt Phase I aus und gibt die Liste der Diffs zurück, View Type als linke Seite, Referenz als rechte. `evaluateDiffs(diffs)` führt Phase II aus: für jeden Diff werden alle Regeln durchlaufen und die zutreffenden Violations gesammelt. `validatePackages(viewType, reference)` verbindet beides und ist die öffentliche Methode, die von außen aufgerufen wird.

Aktuell prüft `evaluateDiffs` noch alle Regeln, unabhängig von ihrer Kategorie. Das ändert sich, sobald die Strictness weiter implementiert ist: `validatePackages` bzw. `evaluateDiffs` sollen dann zusätzlich eine `Strictness` entgegennehmen und nur die Regeln auswerten, deren Kategorie für den gewählten Fall relevant ist.

## Regelklassen

Jede Regel ist eine eigene Klasse und erbt von der abstrakten Klasse `ConformanceRule` (liegt direkt in `rules/`), die Rule-ID, Kategorie und die `check(Diff)`-Methode vorgibt. Dadurch hat jede Regel ihre eigene, feste Kategorie, und die Regeln lassen sich für die Strictness sauber danach aufteilen, welche Kategorien im jeweiligen Fall relevant sind.

Die Regelklassen selbst liegen zusätzlich in drei Unterpaketen, passend zu ihrer Kategorie: `rules/nonbreaking`, `rules/breakingresolvable`, `rules/breakingnotresolvable`. Innerhalb eines Regelpaares hat die Violation-Seite den schlichten Namen (z. B. `FeatureDeletedRule`), die zugehörige konforme Gegenseite im `nonbreaking`-Paket immer den Präfix `Safe` (z. B. `SafeFeatureDeletedRule`).

## Violation

`Violation` ist ein Record mit Rule-ID, Nachricht, Kategorie und dem zugrunde liegenden Diff. Die Kategorie ist entweder Non-Breaking, Breaking-Resolvable oder Breaking-Not-Resolvable. Über diese Kategorien wird später die Strictness gesteuert: welche Kategorien berücksichtigt werden, hängt davon ab, ob der View Type exakt der Referenz entsprechen muss oder nur zu ihr passen muss.

## Was noch fehlt

Die Strictness ist bewusst noch nicht implementiert. Sie soll später steuern, ob ein View Type exakt der Referenz entsprechen muss oder nur zu ihr passen muss, und danach entscheiden, welche Kategorien von Violations überhaupt berücksichtigt werden. Dazu kommt noch die Anbindung an CLI und VS Code, damit die Strictness überhaupt ausgewählt werden kann. Bis das steht, laufen einfach alle Regeln durch, auch die, die eigentlich Non-Breaking wären, das heißt aktuell zeigt VS Code auch bei harmlosen Unterschieden einen Fehler an. Das ist erwartet, kein Bug.

## Anbindung an NeoJoin

Der Validator ist ein eigenständiges Modul und wird von außen an ein paar Stellen eingebunden:

`NeoJoin.xtext` hat eine neue optionale Grammatikregel `Conforms: 'conforms' 'to' uri=STRING;`, referenziert von `ViewTypeDefinition` über `conforms=Conforms?`. Das erlaubt die `conforms to "URI"`-Syntax in einer NeoJoin-Query.

`AQRConforms` (neu, im `model`-Modul) speichert die URI der Referenz. `AQR` hat dafür ein zusätzliches Feld `conforms` bekommen.

`AQRBuilder` liest den `Conforms`-Knoten aus dem AST aus und baut daraus das `AQRConforms`-Objekt für die `AQR`.

`NeoJoinValidator` hat eine neue `@Check`-Methode `checkConformance(ViewTypeDefinition)`: löst die URI über die `EPackage.Registry` zur Referenz auf, generiert den View Type über `MetaModelGenerator`, ruft `ConformanceValidator.validatePackages(...)` auf und meldet jede zurückgegebene `Violation` als Fehler im Editor.
