package tools.vitruv.neojoin.parse;

import org.junit.jupiter.api.Test;

import static tools.vitruv.neojoin.parse.ParseAssertions.assertThat;

public class SubQueryParseTest extends AbstractParseTest {

    @Test
    void simple() {
        var result = parse("""
            from Restaurant r create {
                r.sells create {
                    it.name
                }
            }
            """);

        assertThat(result).hasNoIssues();
    }

    @Test
    void deeplyNested() {
        var result = parse("""
            from rest.Store create {
                it.restaurants create {
                    it.sells create {
                        it.name
                    }
                }
            }
            """);

        assertThat(result).hasNoIssues();
    }

    @Test
    void shadowed() {
        var result = parse("""
            from Restaurant create {
                it.sells create {
                    it.name
                }
            }
            """);

        assertThat(result).hasNoIssues();
    }

    @Test
    void outerNotVisible() {
        var result = parse("""
            from Restaurant r create {
                r.sells create {
                    r.name
                }
            }
            """);

        assertThat(result).hasIssues("The method or field r is undefined");
    }

    @Test
    void featureWithinSubQuery() {
        var result = parse("""
            from rest.Store create {
                it.restaurants create {
                    test := new java.util.concurrent.atomic.AtomicInteger()
                    test := 5
                    name := it.name
                    it.name
                    nameLen = it.name.length
                    optName = it?.name
                    it.sells
                }
            }
            
            from Food f1
            join Food f2
            create MixedFoods {}
            """);

        assertThat(result).hasIssues(
            "Unsupported type: AtomicInteger",
            "Duplicated feature name: test",
            "Duplicated feature name: name",
            "Copy feature expression does not reference a feature",
            "Nullable expression used to initialize non-nullable feature",
            "Inferred type 'MixedFoods' is a query with join which might be unintended and " +
                "can lead to errors during transformation. Use explicit type to clarify the intended type."
        );
    }

    @Test
    void modifierWithinSubQuery() {
        var result = parse("""
            from rest.Store create {
                it.restaurants create {
                    name [changeable, volatile, !changeable, +] = it.name
                    name2 [containment] = it.name
                    sells [id] := it.sells
                }
            }
            """);

        assertThat(result).hasIssues(
            "Duplicated modifier 'changeable'",
            "Cannot assign a single value to a multi-valued feature",
            "Modifier 'containment' is not applicable to attributes",
            "Modifier 'id' is not applicable to references"
        );
    }

    @Test
    void withName() {
        var result = parse("""
            from Restaurant r create {
                r.sells create TastyFood {
                    it.name
                }
            }
            
            create Test { test: TastyFood := null }
            """);

        assertThat(result).hasNoIssues();
    }

    @Test
    void duplicatedNameWithinSubQuery() {
        var result = parse("""
            from Restaurant r create {
                sells1 = r.sells create TastyFood {}
                sells2 = r.sells create TastyFood {}
            
                sells3 = r.sells create {}
                sells4 = r.sells create Food {}
            }
            """);

        assertThat(result).hasIssues(
            "Duplicated target class named 'TastyFood'",
            "Duplicated target class named 'Food'"
        );
    }

    @Test
    void type() {
        var result = parse("""
            from Restaurant r create {
                r.sells create TastyFood {
                    name: EString := it.name
                    price: EFloat := it.name
                    test: Restaurant := it.name
                    test2: Restaurant := it
                    test3: Food := it
                    test4: TastyFood := null
                    test5 := null
                }
            }
            """);

        assertThat(result).hasIssues(
            "Type mismatch: cannot convert from String to float",
            "Type mismatch: cannot convert from EString to Restaurant",
            "Type mismatch: cannot convert from Food to Restaurant",
            "Food cannot be resolved.",
            "Cannot infer type"
        );
    }

    @Test
    void ambiguousImplicitTargetClass() {
        var result = parse("""
            from rest.Store create {
                it.restaurants create {
                    test1 = it.sells create TastyFood {}
                    test3 = it.sells
                }
            }
            
            from Food create NotSoTastyFood
            """);

        assertThat(result).hasIssues(
            "Ambiguous target class for source class 'Food'. Possible candidates: NotSoTastyFood, TastyFood"
        );
    }

    @Test
    void subQueryWithoutBody() {
        var result = parse("""
            from Restaurant r create {
                r.sells create TastyFood
            }
            """);

        assertThat(result).hasNoIssues();
    }

    @Test
    void subQueryWithNullExpression() {
        var result = parse("""
            from Restaurant r create {
                test: Test := null create Test {}
            }
            """);

        assertThat(result).hasIssues("Cannot use null expression for a subquery");
    }

    @Test
    void subQueryWithAttributeExpression() {
        var result = parse("""
            from Restaurant r create {
                test := r.name create Test {}
            }
            """);

        assertThat(result).hasIssues("Cannot use a subquery with an attribute expression (EString)");
    }

    @Test
    void referenceSubQueryType() {
        var result = parse("""
            from Restaurant r create {
                r.sells create TastyFood
                test: TastyFood := r.sells
            }
            """);

        assertThat(result).hasNoIssues();
    }

    @Test
    void referenceOwnSubQueryType() {
        var result = parse("""
            from Restaurant r create {
                test1: Food = r.sells create {}
                test2: TastyFood = r.sells create TastyFood
            }
            """);

        assertThat(result).hasNoIssues();
    }

    @Test
    void referenceIncompleteQueryType() {
        var result = parse("""
            from Restaurant r create {
                test1 := r.sell create TastyFood
                test2: TastyFood := r.sells
            }
            """);

        assertThat(result).hasIssues("The method or field sell is undefined for the type Restaurant");
    }

    @Test
    void referenceIncompatibleQueryType() {
        var result = parse("""
            from Restaurant r create {
                test1 := r.sells create TastyFood
                test2: TastyFood := r
            }
            """);

        assertThat(result).hasIssues("Type mismatch: cannot convert from Restaurant to TastyFood");
    }

    @Test
    void referenceIncompatibleQueryTypeWithSubQuery() {
        var result = parse("""
            from Restaurant r create {
                test: Food := r.sells create TastyFood
            }
            
            from Food create
            """);

        assertThat(result).hasIssues("Type mismatch: explicit type does not match subquery type");
    }

}
