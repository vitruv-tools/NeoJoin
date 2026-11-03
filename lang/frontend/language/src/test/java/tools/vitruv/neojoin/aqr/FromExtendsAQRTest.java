package tools.vitruv.neojoin.aqr;

import org.junit.jupiter.api.Test;
import tools.vitruv.neojoin.AbstractKennelTest;

import static tools.vitruv.neojoin.aqr.AQRAssertions.assertThat;
import static tools.vitruv.neojoin.aqr.AQRTargetClassAssertions.assertThat;

class FromExtendsAQRTest extends AbstractKennelTest {

    /**
     * {@code from extends} sets {@code includeSubClasses} (with alias and condition, and without alias and body), a
     * plain {@code from} does not.
     */
    @Test
    void fromExtendsIncludesSubClasses() {
        var aqr = parse("""
            from extends Animal a
            where a.name.startsWith("A")
            create Pet {
                a.name
            }

            from Dog d create Doggo {
                d.breed
            }

            from extends Cat create
            """);

        assertThat(aqr)
            .hasTargetClass(
                "Pet", pet -> {
                    assertThat(pet)
                        .sourceIs(lookup("kennel", "Animal"), "a")
                        .sourceIncludesSubClasses()
                        .hasCondition()
                        .hasNoMoreJoins()
                        .hasCopiedAttribute("name", lookup("kennel", "Animal", "name"))
                        .hasNoMoreFeatures();
                }
            )
            .hasTargetClass(
                "Doggo", doggo -> {
                    assertThat(doggo)
                        .sourceIs(lookup("kennel", "Dog"), "d")
                        .sourceExcludesSubClasses()
                        .hasCopiedAttribute("breed", lookup("kennel", "Dog", "breed"))
                        .hasNoMoreFeatures();
                }
            )
            .hasTargetClass(
                "Cat", cat -> {
                    assertThat(cat)
                        .sourceIs(lookup("kennel", "Cat"))
                        .sourceIncludesSubClasses()
                        .hasCopiedAttribute("name", lookup("kennel", "Animal", "name")) // inherited source feature
                        .hasCopiedAttribute("lives", lookup("kennel", "Cat", "lives"))
                        .hasNoMoreFeatures();
                }
            )
            .hasNoMoreTargetClasses();
    }

    /**
     * {@code join extends} sets {@code includeSubClasses} of the joined source only.
     */
    @Test
    void joinExtendsSelectsSubClassesOfJoinedSourceOnly() {
        var aqr = parse("""
            from Kennel k
            join extends Dog d
            join Cat c
            create Mix {
                n = k.name
                b = d.breed
                l = c.lives
            }
            """);

        assertThat(aqr)
            .hasTargetClass(
                "Mix", mix -> {
                    assertThat(mix)
                        .sourceIs(lookup("kennel", "Kennel"), "k")
                        .sourceExcludesSubClasses()
                        .hasJoin(lookup("kennel", "Dog"), "d", AQRJoin.Type.Inner)
                        .joinIncludesSubClasses(lookup("kennel", "Dog"), "d", true)
                        .hasJoin(lookup("kennel", "Cat"), "c", AQRJoin.Type.Inner)
                        .joinIncludesSubClasses(lookup("kennel", "Cat"), "c", false)
                        .hasNoMoreJoins()
                        .hasCopiedAttribute("n", lookup("kennel", "Kennel", "name"))
                        .hasCopiedAttribute("b", lookup("kennel", "Dog", "breed"))
                        .hasCopiedAttribute("l", lookup("kennel", "Cat", "lives"))
                        .hasNoMoreFeatures();
                }
            )
            .hasNoMoreTargetClasses();
    }

}
