package tools.vitruv.neojoin.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AssertionsTest {

    @Test
    void checkThrowsAnAssertionErrorOnlyIfTheConditionIsFalse() {
        assertThatCode(() -> Assertions.check(true)).doesNotThrowAnyException();
        assertThatThrownBy(() -> Assertions.check(false)).isInstanceOf(AssertionError.class).hasMessage(null);
        assertThatThrownBy(() -> Assertions.check(false, "message"))
            .isInstanceOf(AssertionError.class)
            .hasMessage("message");
        assertThatThrownBy(() -> Assertions.check(false, () -> "supplied"))
            .isInstanceOf(AssertionError.class)
            .hasMessage("supplied");
    }

    @Test
    void requireThrowsAnIllegalArgumentExceptionOnlyIfTheConditionIsFalse() {
        assertThatCode(() -> Assertions.require(true)).doesNotThrowAnyException();
        assertThatThrownBy(() -> Assertions.require(false)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Assertions.require(false, "message"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("message");
        assertThatThrownBy(() -> Assertions.require(false, () -> "supplied"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("supplied");
    }

    @Test
    void failAlwaysThrowsAnAssertionError() {
        assertThatThrownBy(Assertions::fail).isInstanceOf(AssertionError.class);
        assertThatThrownBy(() -> Assertions.fail("message"))
            .isInstanceOf(AssertionError.class)
            .hasMessage("message");
    }

}
