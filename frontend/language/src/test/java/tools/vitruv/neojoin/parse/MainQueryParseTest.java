package tools.vitruv.neojoin.parse;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static tools.vitruv.neojoin.parse.ParseAssertions.assertThat;

class MainQueryParseTest extends AbstractParseTest {

    @Override
    protected List<String> getMetaModelPaths() {
        var paths = new ArrayList<>(super.getMetaModelPaths());
        paths.add("/models/wallet.ecore");
        paths.add("/models/bank.ecore");
        return paths;
    }

    @Test
    void withoutTargetName() {
        var result = parse("""
            from Restaurant create {}
            """);

        assertThat(result).hasNoIssues();
    }

    @Test
    void withTargetName() {
        var result = parse("""
            from Restaurant create Rest {}
            """);

        assertThat(result).hasNoIssues();
    }

    @Test
    void condition() {
        var result = parse("""
            from Restaurant r
            where r.name.startsWith("Pizzeria")
            create {}
            """);

        assertThat(result).hasNoIssues();
    }
    
    @Test
    void multipleConditions() {
        var result = parse("""
            from Restaurant r
            where r.name.startsWith("Pizzeria")
            where r.name.endsWith("Toni")
            create {}
            """);

        assertThat(result).hasNoIssues();
    }

    @Test
    void nonBooleanCondition() {
        var result = parse("""
            from Restaurant r
            where r.name
            create {}
            """);

        assertThat(result).hasExactlyIssues("Type mismatch: cannot convert from String to boolean");
    }

    @Test
    void nonBooleanAdditionalCondition() {
        var result = parse("""
            from Restaurant r
            where r.name.startsWith("Pizzeria")
            where r.name
            create {}
            """);

        assertThat(result).hasExactlyIssues("Type mismatch: cannot convert from String to boolean");
    }

    @Test
    void duplicateTargetName() {
        var result = parse("""
            from Restaurant create Rest {}
            from Food create Rest {}
            """);

        var message = "Duplicated target class named 'Rest'";
        assertThat(result)
            .hasIssueAt(message, "Rest", "Rest")
            .hasExactlyIssues(message);
    }

    @Test
    void duplicateTargetNameImplicit() {
        var result = parse("""
            from Restaurant create {}
            from Food create Restaurant {}
            """);

        var message = "Duplicated target class named 'Restaurant'";
        assertThat(result)
            .hasIssueAt(message, "from Restaurant create {}", "Restaurant")
            .hasExactlyIssues(message);
    }

    @Test
    void implicitClassWithNameOfTargetClassIsRejected() {
        var result = parse("""
            from Restaurant r create {
                r.sells
            }

            create Food {}
            """);

        var implicitClassMessage = "Implicit class of source class 'Food' has the same name as the target "
            + "class 'Food'. Either choose a different name for the target class "
            + "or create a query for the source class that specifies a different target class name.";
        var targetClassMessage = "Target class 'Food' has the same name as the implicit class of source class "
            + "'Food'. Either choose a different name for this class "
            + "or create a query for the source class that specifies a different target class name.";
        assertThat(result)
            .hasIssueAt(implicitClassMessage, "r.sells")
            .hasIssueAt(targetClassMessage, "Food")
            .hasExactlyIssues(implicitClassMessage, targetClassMessage);
    }

    @Test
    void implicitClassWithNameOfQueryWithoutNameIsRejected() {
        var result = internalParse("""
            export package to "http://example.com"

            import "http://example.org/restaurant" as rest
            import "http://example.org/menu"

            from Restaurant r create {
                r.sells
            }

            from menu.Food create
            """);

        var implicitClassMessage = "Implicit class of source class 'rest.Food' has the same name as the target "
            + "class 'Food'. Either choose a different name for the target class "
            + "or create a query for the source class that specifies a different target class name.";
        var targetClassMessage = "Target class 'Food' has the same name as the implicit class of source class "
            + "'rest.Food'. Either choose a different name for this class "
            + "or create a query for the source class that specifies a different target class name.";
        assertThat(result)
            .hasIssueAt(implicitClassMessage, "r.sells")
            .hasIssueAt(targetClassMessage, "from menu.Food create")
            .hasExactlyIssues(implicitClassMessage, targetClassMessage);
    }

    /**
     * An abstract class without source is only used instead of the implicit class of an abstract source class: the
     * instances of a concrete source class need a concrete target class.
     */
    @Test
    void implicitClassOfConcreteSourceClassWithNameOfAbstractClassIsRejected() {
        var result = parse("""
            from Restaurant r create {
                r.sells
            }

            abstract Food {
                name: EString
            }
            """);

        var implicitClassMessage = "Implicit class of source class 'Food' has the same name as the target "
            + "class 'Food'. Either choose a different name for the target class "
            + "or create a query for the source class that specifies a different target class name.";
        var targetClassMessage = "Target class 'Food' has the same name as the implicit class of source class "
            + "'Food'. Either choose a different name for this class "
            + "or create a query for the source class that specifies a different target class name.";
        assertThat(result)
            .hasIssueAt(implicitClassMessage, "r.sells")
            .hasIssueAt(targetClassMessage, "Food")
            .hasExactlyIssues(implicitClassMessage, targetClassMessage);
    }

    @Test
    void implicitClassesOfSourceClassesWithSameNameAreRejected() {
        var result = internalParse("""
            export package to "http://example.com"

            import "http://example.org/restaurant" as rest
            import "http://example.org/menu"

            from Restaurant create
            from Root create Category
            """);

        var message = "Implicit classes of the source classes menu.Food, rest.Food have the same name "
            + "'Food'. For all but one of these source classes, "
            + "create a query that specifies a different target class name.";
        assertThat(result)
            .hasIssueAt(message, "from Restaurant create", "from Root create Category")
            .hasExactlyIssues(message);
    }

    /**
     * The packages of the source classes are not imported and have the same name, so their namespace URIs tell them
     * apart.
     */
    @Test
    void implicitClassesOfSourceClassesInPackagesWithSameNameAreRejected() {
        var result = internalParse("""
            export package to "http://example.com"

            import "http://example.org/order"

            from Order create
            """);

        var message = "Implicit classes of the source classes supplier.Food (http://example.org/supplier/a), "
            + "supplier.Food (http://example.org/supplier/b) have the same name "
            + "'Food'. For all but one of these source classes, "
            + "create a query that specifies a different target class name.";
        assertThat(result)
            .hasIssueAt(message, "from Order create")
            .hasExactlyIssues(message);
    }

    @Test
    void targetClassWithNameOfSeveralImplicitClassesIsReportedOnce() {
        var result = internalParse("""
            export package to "http://example.com"

            import "http://example.org/restaurant"
            import "http://example.org/menu"

            from Restaurant create
            from Root create Category
            create Food {}
            """);

        var sameNameMessage = "Implicit classes of the source classes menu.Food, restaurant.Food have the same "
            + "name 'Food'. For all but one of these source classes, "
            + "create a query that specifies a different target class name.";
        var restaurantMessage = "Implicit class of source class 'restaurant.Food' has the same name as the "
            + "target class 'Food'. Either choose a different name for the target class "
            + "or create a query for the source class that specifies a different target class name.";
        var menuMessage = "Implicit class of source class 'menu.Food' has the same name as the "
            + "target class 'Food'. Either choose a different name for the target class "
            + "or create a query for the source class that specifies a different target class name.";
        var targetClassMessage = "Target class 'Food' has the same name as the implicit classes of the source "
            + "classes menu.Food, restaurant.Food. Either choose a different name for this class "
            + "or create a query for the source class that specifies a different target class name.";
        assertThat(result)
            .hasIssueAt(sameNameMessage, "from Restaurant create", "from Root create Category")
            .hasIssueAt(restaurantMessage, "from Restaurant create")
            .hasIssueAt(menuMessage, "from Root create Category")
            .hasIssueAt(targetClassMessage, "Food")
            .hasExactlyIssues(sameNameMessage, restaurantMessage, menuMessage, targetClassMessage);
    }

    @Test
    void queryWithoutFeatures() {
        var result = parse("""
            from Food create
            """);

        assertThat(result).hasNoIssues();
    }

    @Test
    void queryWithoutHeaderDoesNotCrash() {
        var result = internalParse("""
            from Restaurant create
            """);

        assertThat(result)
            .hasExactlyIssues("mismatched input 'from' expecting 'export'", "Restaurant cannot be resolved.");
    }

    @Test
    void queryWithNoSource() {
        var result = parse("""
            create Result {
                test := 5 + 7
                test2: Food := null
            }

            from Food create
            """);

        assertThat(result)
            .hasNoIssues();
    }

    @Test
    void queryWithoutSourceAndWithoutName() {
        var result = parse("""
            create {}
            """);

        assertThat(result)
            .hasExactlyIssues("Query without source must have a target name");
    }

    @Test
    void queryWithoutSourceAndWithoutBody() {
        var result = parse("""
            create Result
            """);

        assertThat(result)
            .hasExactlyIssues("Query without source must have a body");
    }

    @Test
    void referenceToQueryWithoutSource() {
        var result = parse("""
            from Restaurant r create {
                test: Result := r
            }

            create Result {}
            """);

        assertThat(result)
            .hasExactlyIssues("Type mismatch: cannot convert from Restaurant to Result");
    }

    /**
     * {@code create} followed by {@code abstract} is a concrete query without name and body, followed by an abstract
     * query. The correct syntax is {@code abstract A { ... }}.
     */
    @Test
    void createWithoutSourceNameAndBodyIsRejected() {
        var result = parse("""
            create abstract A {
                x: EString
            }
            """);

        assertThat(result).hasExactlyIssues("Incomplete query: 'create' needs a source, or a target name and a body");
    }

    @Test
    void targetClassWithNameOfImportedDataTypeIsReported() {
        var result = parse("""
            from Food f create Money {
                f.name
            }
            """);

        assertThat(result)
            .hasExactlyIssues("Target class 'Money' has the same name as the data type 'Money'. "
                + "Choose a different name for this class.");
    }

    @Test
    void targetClassWithNameOfEcoreDataTypeIsReported() {
        var result = parse("""
            from Food f create EString {
                f.name
            }
            """);

        assertThat(result)
            .hasExactlyIssues("Target class 'EString' has the same name as the data type 'EString'. "
                + "Choose a different name for this class.");
    }

    @Test
    void targetClassWithNameOfSeveralDataTypesListsAllOfThem() {
        var result = internalParse("""
            export package to "http://example.com"

            import "http://example.org/restaurant" as rest
            import "http://example.org/bank"

            from Food f create Money {
                f.name
            }
            """);

        assertThat(result).hasExactlyIssues(
            "Target class 'Money' has the same name as the data types bank.Money, rest.Money. "
                + "Choose a different name for this class."
        );
    }

    @Test
    void implicitClassWithNameOfImportedDataTypeIsReported() {
        var result = internalParse("""
            export package to "http://example.com"

            import "http://example.org/restaurant" as rest
            import "http://example.org/wallet"

            from Wallet w create {
                w.cash
            }
            """);

        var message = "Implicit class of source class 'Money' has the same name as the data type "
            + "'Money'. Create a query for the source class that specifies a different target class name.";
        assertThat(result)
            .hasIssueAt(message, "w.cash")
            .hasExactlyIssues(message);
    }

}
