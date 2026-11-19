package tools.vitruv.neojoin.collector;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CollectorIssueTest {

    @Test
    void loadFailureOfExceptionWithoutMessageNamesTheException() {
        var exception = new RuntimeException("wrapper", new IllegalStateException());

        var issue = CollectorIssue.LoadFailure.of("file:///models/a.ecore", exception);

        assertThat(issue).hasToString("Cannot load 'file:///models/a.ecore': IllegalStateException");
    }

}
