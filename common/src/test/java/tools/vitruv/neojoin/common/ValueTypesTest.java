package tools.vitruv.neojoin.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ValueTypesTest {

    @Test
    void pairHoldsBothValues() {
        var pair = new Pair<>("left", 2);

        assertThat(pair.left()).isEqualTo("left");
        assertThat(pair.right()).isEqualTo(2);
        assertThat(pair).isEqualTo(new Pair<>("left", 2));
    }

    @Test
    void resultIsEitherSuccessOrFailure() {
        var failure = new IllegalStateException();

        assertThat(new Result.Success<>(1).value()).isEqualTo(1);
        assertThat(new Result.Failure<Integer>(failure).throwable()).isSameAs(failure);
    }

    @Test
    void mutableCanBeChangedFromALambda() {
        var counter = new Mutable<>(0);
        Runnable increment = () -> counter.value++;
        increment.run();
        increment.run();

        assertThat(counter.value).isEqualTo(2);
    }

}
