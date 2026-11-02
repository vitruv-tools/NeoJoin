package tools.vitruv.neojoin.aqr;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import tools.vitruv.neojoin.AbstractKennelTest;
import tools.vitruv.neojoin.aqr.AQRFeatureOptionsAssertions.Mod;

import static org.assertj.core.api.Assertions.assertThat;
import static tools.vitruv.neojoin.aqr.AQRAssertions.assertThat;
import static tools.vitruv.neojoin.aqr.AQRFeatureOptionsAssertions.assertThat;
import static tools.vitruv.neojoin.aqr.AQRTargetClassAssertions.assertThat;

public class InheritanceAQRTest extends AbstractKennelTest {

    private static final int Unbounded = -1;

    private static AQRTargetClass find(AQR aqr, String name) {
        return aqr.classes().stream().filter(c -> c.name().equals(name)).findFirst().orElseThrow();
    }

    /**
     * Explicit copy and calculation override abstract features and take all modifiers from the overridden feature.
     */
    @Test
    void overridingFeaturesInheritModifiers() {
        var aqr = parse("""
            abstract Named {
                name: EString [1, volatile, !changeable]
                tags: EString [*, !unique]
                label: EString [?]
            }

            from Dog d create Doggo extends Named {
                name = d.name
                tags := #[d.name, d.breed]
                label = d.name
                breed = d.breed
            }
            """);

        assertThat(aqr)
            .hasTargetClass("Named", named -> assertThat(named).isAbstract())
            .hasTargetClass(
                "Doggo", doggo -> {
                    assertThat(doggo)
                        .isNotAbstract()
                        .hasSuperClasses("Named")
                        .hasOverridingCopiedAttribute(
                            "name", lookup("kennel", "Animal", "name"), options -> {
                                assertThat(options)
                                    .hasBounds(1, 1)
                                    .is(Mod.Volatile)
                                    .isNot(Mod.Changeable)
                                    .otherModifiersHaveDefaultValue();
                            }
                        )
                        .hasOverridingCalculatedAttribute(
                            "tags", "EString", options -> {
                                assertThat(options)
                                    .hasBounds(0, Unbounded)
                                    .isNot(Mod.Unique)
                                    .otherModifiersHaveDefaultValue();
                            }
                        )
                        .hasOverridingCopiedAttribute(
                            "label", lookup("kennel", "Animal", "name"), options -> {
                                // the source feature is required, but the overridden feature defines the modifiers
                                assertThat(options).hasBounds(0, 1);
                            }
                        )
                        .hasCopiedAttribute("breed", lookup("kennel", "Dog", "breed")) // not an override
                        .hasNoMoreFeatures();
                }
            )
            .hasNoMoreTargetClasses();
    }

    /**
     * Implicit copies override features of abstract and concrete (source-less) super classes.
     */
    @Test
    void implicitCopyOverridesAbstractAndConcreteFeatures() {
        var aqr = parse("""
            abstract Lived {
                lives: EInt [1, !changeable]
            }

            create Base {
                lives: EInt [1] := 9
            }

            from Cat create Tabby extends Lived

            from Cat create Lion extends Base
            """);

        assertThat(aqr)
            .hasTargetClass(
                "Base", base -> {
                    assertThat(base)
                        .isNotAbstract()
                        .hasNoSource()
                        .hasCalculatedAttribute("lives", "EInt")
                        .hasNoMoreFeatures();
                }
            )
            .hasTargetClass("Lived", lived -> assertThat(lived).isAbstract())
            .hasTargetClass(
                "Tabby", tabby -> {
                    assertThat(tabby)
                        .hasSuperClasses("Lived")
                        .hasCopiedAttribute("name", lookup("kennel", "Animal", "name")) // inherited source feature
                        .hasOverridingImplicitlyCopiedAttribute(
                            "lives", lookup("kennel", "Cat", "lives"), options -> {
                                assertThat(options)
                                    .hasBounds(1, 1)
                                    .isNot(Mod.Changeable)
                                    .otherModifiersHaveDefaultValue();
                            }
                        )
                        .hasNoMoreFeatures();
                }
            )
            .hasTargetClass(
                "Lion", lion -> {
                    assertThat(lion)
                        .hasSuperClasses("Base")
                        .hasCopiedAttribute("name", lookup("kennel", "Animal", "name")) // inherited source feature
                        .hasOverridingImplicitlyCopiedAttribute(
                            "lives", lookup("kennel", "Cat", "lives"), options -> {
                                assertThat(options).hasBounds(1, 1);
                            }
                        )
                        .hasNoMoreFeatures();
                }
            )
            .hasNoMoreTargetClasses();
    }

    /**
     * Multiple, transitive and forward declared super classes.
     */
    @Test
    void multipleTransitiveAndForwardDeclaredSuperClasses() {
        var aqr = parse("""
            from Puppy p create Pup extends Aged, Tagged {
                age = p.age
                name = p.name
                tag := "x"
            }

            abstract Aged {
                age: EInt
            }

            abstract Tagged extends Named {
                name: EString
                tag: EString
            }

            abstract Named {
                name: EString
            }
            """);

        assertThat(aqr)
            .hasTargetClass(
                "Named", named -> {
                    assertThat(named)
                        .isAbstract()
                        .hasNoSuperClasses()
                        .hasAbstractAttribute("name", "EString")
                        .hasNoMoreFeatures();
                }
            )
            .hasTargetClass("Aged", aged -> assertThat(aged).hasNoSuperClasses().hasAbstractAttribute("age", "EInt"))
            .hasTargetClass(
                "Tagged", tagged -> {
                    assertThat(tagged)
                        .hasSuperClasses("Named")
                        .hasOverridingAbstractAttribute("name", "EString")
                        .hasAbstractAttribute("tag", "EString")
                        .hasNoMoreFeatures();
                }
            )
            .hasTargetClass(
                "Pup", pup -> {
                    assertThat(pup)
                        .hasSuperClasses("Aged", "Tagged")
                        .hasAllSuperClasses("Aged", "Tagged", "Named")
                        .hasOverridingCopiedAttribute("age", lookup("kennel", "Puppy", "age"))
                        .hasOverridingCopiedAttribute("name", lookup("kennel", "Animal", "name"))
                        .hasOverridingCalculatedAttribute("tag", "EString")
                        .hasNoMoreFeatures();
                }
            )
            .hasNoMoreTargetClasses();
    }

    /**
     * Reference overrides whose value has the same type as the overridden feature or a sub type of it (where the sub
     * type extends an explicit query that selects the sub classes of a source class). In both cases, the overriding
     * feature has the type of the overridden feature.
     */
    @Test
    void referenceOverridesWithSameTypeAndSubType() {
        var aqr = parse("""
            from extends Animal a create Pet {
                a.name
            }

            from Dog d create Doggy extends Pet {
                name = d.name
            }

            abstract Home {
                residents: Pet [*, !ordered]
            }

            from Kennel k create KennelA extends Home {
                residents = k.animals
            }

            from Kennel k create KennelB extends Home {
                residents = k.dogs
            }
            """);

        assertThat(aqr)
            .hasTargetClass(
                "Pet", pet -> {
                    assertThat(pet)
                        .sourceIs(lookup("kennel", "Animal"), "a")
                        .sourceIncludesSubClasses()
                        .hasNoSuperClasses();
                }
            )
            .hasTargetClass(
                "Doggy", doggy -> {
                    assertThat(doggy)
                        .sourceExcludesSubClasses()
                        .hasSuperClasses("Pet")
                        .hasOverridingCopiedAttribute(
                            "name", lookup("kennel", "Animal", "name"), options -> {
                                assertThat(options).hasBounds(1, 1);
                            }
                        )
                        .hasNoMoreFeatures();
                }
            )
            .hasTargetClass("Home", home -> assertThat(home).hasAbstractReference("residents", "Pet"))
            .hasTargetClass(
                "KennelA", kennel -> {
                    assertThat(kennel)
                        .hasOverridingCopiedReference(
                            "residents", "Pet", lookup("kennel", "Kennel", "animals"), options -> {
                                assertThat(options)
                                    .hasBounds(0, Unbounded)
                                    .isNot(Mod.Ordered)
                                    .otherModifiersHaveDefaultValue();
                            }
                        )
                        .hasNoMoreFeatures();
                }
            )
            .hasTargetClass(
                "KennelB", kennel -> {
                    assertThat(kennel)
                        .hasOverridingCopiedReference(
                            "residents", "Pet", lookup("kennel", "Kennel", "dogs"), options -> {
                                assertThat(options).hasBounds(0, Unbounded).isNot(Mod.Ordered);
                            }
                        )
                        .hasNoMoreFeatures();
                }
            )
            .hasNoMoreTargetClasses();
    }

    /**
     * A reference can be explicitly typed with a super class of the target class of the inferred type.
     */
    @Test
    void referenceTypedWithSuperClass() {
        var aqr = parse("""
            abstract Named {
                name: EString
            }

            from Dog d create Doggy extends Named {
                name = d.name
            }

            from Kennel k create KennelT {
                dogs: Named := k.dogs
            }
            """);

        assertThat(aqr)
            .hasTargetClass("Named", named -> {})
            .hasTargetClass("Doggy", doggy -> assertThat(doggy).hasSuperClasses("Named"))
            .hasTargetClass("KennelT", kennel -> assertThat(kennel).hasCalculatedReference("dogs", "Named"))
            .hasNoMoreTargetClasses();
    }

    /**
     * A diamond of classes without features is valid; {@code allSuperClasses()} lists each ancestor once. (A diamond is
     * rejected if a feature is inherited via both paths, see
     * {@code InheritanceParseTest.sameFeatureFromCommonAncestor}.)
     */
    @Test
    void allSuperClassesOfDiamondHaveNoDuplicatesAndCanBeCalledRepeatedly() {
        var aqr = parse("""
            abstract A {}
            abstract B extends A {}
            abstract C extends A {}
            abstract D extends B, C {}
            """);

        var d = find(aqr, "D");
        for (var i = 0; i < 2; i++) {
            assertThat(d.allSuperClasses()).extracting(AQRTargetClass::name).containsExactlyInAnyOrder("A", "B", "C");
        }
        assertThat(d.superClasses()).extracting(AQRTargetClass::name).containsExactly("B", "C");
        assertThat(find(aqr, "B").superClasses()).extracting(AQRTargetClass::name).containsExactly("A");
    }

    /**
     * Main queries without body and subqueries can be super classes.
     */
    @Test
    void superClassFromMainQueryWithoutBodyAndFromSubQuery() {
        var aqr = parse("""
            from Dog create DogT

            from Puppy p create PuppyT extends DogT {
                name = p.name
                breed = p.breed
                age = p.age
            }

            from Kennel k create KT {
                k.animals create AnimalS
            }

            from Dog d create DogS extends AnimalS {
                name = d.name
                breed = d.breed
            }
            """);

        assertThat(aqr)
            .hasTargetClass("DogT", dogT -> assertThat(dogT).sourceIs(lookup("kennel", "Dog")).hasNoSuperClasses())
            .hasTargetClass(
                "PuppyT", puppyT -> {
                    assertThat(puppyT)
                        .hasSuperClasses("DogT")
                        .hasOverridingCopiedAttribute(
                            "breed", lookup("kennel", "Dog", "breed"), options -> {
                                assertThat(options).hasBounds(0, 1).otherModifiersHaveDefaultValue();
                            }
                        )
                        .hasFeatureNames("name", "breed", "age");
                }
            )
            .hasTargetClass(
                "AnimalS", animalS -> {
                    assertThat(animalS)
                        .sourceIs(lookup("kennel", "Animal"))
                        .hasCopiedAttribute("name", lookup("kennel", "Animal", "name"))
                        .hasNoMoreFeatures();
                }
            )
            .hasTargetClass(
                "DogS", dogS -> {
                    assertThat(dogS)
                        .hasSuperClasses("AnimalS")
                        .hasOverridingCopiedAttribute(
                            "name", lookup("kennel", "Animal", "name"), options -> {
                                assertThat(options).hasBounds(1, 1);
                            }
                        )
                        .hasCopiedAttribute("breed", lookup("kennel", "Dog", "breed"))
                        .hasNoMoreFeatures();
                }
            )
            .hasTargetClass("KT", kt -> assertThat(kt).hasCopiedReference("animals", "AnimalS", lookup("kennel", "Kennel", "animals")))
            .hasNoMoreTargetClasses();
    }

    /**
     * The root can extend a class: it keeps its super class and gets the root references for all other non-abstract
     * classes, but none for the abstract super class.
     */
    @Test
    void rootWithSuperClass() {
        var aqr = parse("""
            abstract Named {
                name: EString
            }

            create root R extends Named {
                name := "r"
            }

            from Cat create Tabby
            """);

        assertThat(aqr)
            .hasRootTargetClass(
                "R", root -> {
                    assertThat(root)
                        .hasSuperClasses("Named")
                        .hasOverridingCalculatedAttribute("name", "EString")
                        .hasGeneratedReference("tabbys", "Tabby")
                        .hasNoMoreFeatures();
                }
            )
            .hasTargetClass("Named", named -> assertThat(named).isAbstract())
            .hasTargetClass("Tabby", tabby -> {})
            .hasNoMoreTargetClasses();
    }

    /**
     * Enum types of abstract features are collected as data types.
     */
    @Test
    void enumTypeOfAbstractFeature() {
        var aqr = parse("""
            abstract Specialized {
                specialty: Specialty
            }

            from Vet v create Doctor extends Specialized {
                specialty = v.specialty
            }
            """);

        assertThat(aqr.dataTypes()).containsExactly((org.eclipse.emf.ecore.EDataType) lookup("kennel", "Specialty"));
        assertThat(aqr)
            .hasTargetClass("Specialized", specialized -> assertThat(specialized).hasAbstractAttribute("specialty", "Specialty"))
            .hasTargetClass(
                "Doctor", doctor -> {
                    assertThat(doctor)
                        .hasSuperClasses("Specialized")
                        .hasOverridingCopiedAttribute("specialty", lookup("kennel", "Vet", "specialty"))
                        .hasNoMoreFeatures();
                }
            )
            .hasNoMoreTargetClasses();
    }

    /**
     * {@code allSuperClasses()} does not change the super classes of any class: for {@code D extends A, B} with
     * {@code B extends A}, B still extends A afterwards.
     */
    @Test
    void allSuperClassesDoesNotMutateSuperClassesOfSuperClasses() {
        var aqr = parse("""
            abstract A {}
            abstract B extends A {}
            from Animal create D extends A, B {}
            """);

        var d = find(aqr, "D");
        assertThat(d.allSuperClasses()).extracting(AQRTargetClass::name).containsExactlyInAnyOrder("A", "B");

        assertThat(find(aqr, "B").superClasses())
            .as("B should still extend A after calling D.allSuperClasses()")
            .extracting(AQRTargetClass::name)
            .containsExactly("A");
        assertThat(d.superClasses()).extracting(AQRTargetClass::name).containsExactly("A", "B");
    }

    /**
     * The modifiers of an overriding feature are those of the root of the override chain ({@code A.name} is {@code [1]},
     * so {@code C.name} is {@code [1]}). The result must not depend on the order in which the classes are processed,
     * hence the repetition.
     */
    @RepeatedTest(20)
    void transitiveOverrideInheritsModifiersOfRootDeclaration() {
        var aqr = parse("""
            create A {
                name: EString [1] := "a"
            }

            create B extends A {
                name := "b"
            }

            create C extends B {
                name := "c"
            }
            """);

        assertThat(find(aqr, "B").features().get(0).options()).hasBounds(1, 1);
        // C.name must get the modifiers of A.name via B.name
        assertThat(find(aqr, "C").features().get(0).options()).hasBounds(1, 1);
    }

    /**
     * An {@code Override} kind reports the source of the overriding copy.
     */
    @Test
    void overrideKindDelegatesSourceToOverridingKind() {
        var aqr = parse("""
            abstract Lived {
                lives: EInt
            }

            from Cat c create Tabby extends Lived {
                lives = c.lives
            }
            """);

        var kind = find(aqr, "Tabby").features().get(0).kind();
        assertThat(kind).isInstanceOf(AQRFeature.Kind.Override.class);
        assertThat(kind.source()).isSameAs(lookup("kennel", "Cat", "lives"));
        assertThat(kind.expression()).isNotNull();
    }

}
