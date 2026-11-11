package tools.vitruv.neojoin.common;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TypeCastsTest {

    @Test
    void numbersAndCharactersCanBeCastIntoEachOtherButBooleansCannot() {
        assertThat(TypeCasts.canCast(int.class, Integer.class)).isTrue();
        assertThat(TypeCasts.canCast(int.class, long.class)).isTrue();
        assertThat(TypeCasts.canCast(long.class, char.class)).isTrue();
        assertThat(TypeCasts.canCast(char.class, double.class)).isTrue();
        assertThat(TypeCasts.canCast(boolean.class, Boolean.class)).isTrue();
        assertThat(TypeCasts.canCast(boolean.class, int.class)).isFalse();
        assertThat(TypeCasts.canCast(int.class, boolean.class)).isFalse();
    }

    @Test
    void otherTypesFollowJavaAssignmentSemantics() {
        assertThat(TypeCasts.canCast(ArrayList.class, List.class)).isTrue();
        assertThat(TypeCasts.canCast(List.class, ArrayList.class)).isFalse();
        assertThat(TypeCasts.canCast(String.class, int.class)).isFalse();
    }

    @Test
    void voidCannotBeCast() {
        assertThatThrownBy(() -> TypeCasts.canCast(void.class, int.class)).isInstanceOf(AssertionError.class);
    }

    @Test
    void numbersAreConvertedToTheTargetType() {
        assertThat(TypeCasts.cast(5, byte.class)).isEqualTo((byte) 5);
        assertThat(TypeCasts.cast(5, short.class)).isEqualTo((short) 5);
        assertThat(TypeCasts.cast(5, Integer.class)).isEqualTo(5);
        assertThat(TypeCasts.cast(5, long.class)).isEqualTo(5L);
        assertThat(TypeCasts.cast(5, float.class)).isEqualTo(5f);
        assertThat(TypeCasts.cast(5L, double.class)).isEqualTo(5d);
        assertThat(TypeCasts.cast(65, char.class)).isEqualTo('A');
    }

    @Test
    void charactersAreConvertedToNumbers() {
        assertThat(TypeCasts.cast('A', byte.class)).isEqualTo((byte) 65);
        assertThat(TypeCasts.cast('A', short.class)).isEqualTo((short) 65);
        assertThat(TypeCasts.cast('A', int.class)).isEqualTo(65);
        assertThat(TypeCasts.cast('A', long.class)).isEqualTo(65L);
        assertThat(TypeCasts.cast('A', float.class)).isEqualTo(65f);
        assertThat(TypeCasts.cast('A', double.class)).isEqualTo(65d);
    }

    @Test
    void valuesOfTheTargetTypeAndNonNumericTypesAreReturnedUnchanged() {
        var list = new ArrayList<String>();
        assertThat(TypeCasts.cast(true, boolean.class)).isEqualTo(true);
        assertThat(TypeCasts.cast("text", String.class)).isEqualTo("text");
        assertThat(TypeCasts.cast(list, List.class)).isSameAs(list);
    }

    @Test
    void impossibleCastsFail() {
        assertThatThrownBy(() -> TypeCasts.cast(true, int.class)).isInstanceOf(AssertionError.class);
    }

}
