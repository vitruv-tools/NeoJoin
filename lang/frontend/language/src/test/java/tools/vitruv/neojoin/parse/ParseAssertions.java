package tools.vitruv.neojoin.parse;

import org.assertj.core.api.AbstractAssert;
import org.eclipse.xtext.diagnostics.Severity;
import org.eclipse.xtext.validation.Issue;
import tools.vitruv.neojoin.ast.ViewTypeDefinition;
import tools.vitruv.neojoin.utils.Pair;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ParseAssertions extends AbstractAssert<ParseAssertions, List<Issue>> {

    protected ParseAssertions(List<Issue> actual) {
        super(actual, ParseAssertions.class);
    }

    public static ParseAssertions assertThat(Pair<ViewTypeDefinition, List<Issue>> actual) {
        //noinspection DataFlowIssue - false positive
        return new ParseAssertions(actual.right());
    }

    private static String formatMessageList(String header, List<String> issues) {
        if (issues.isEmpty()) {
            return header + ": <no issues>";
        } else {
            return header + ":\n" + issues.stream()
                .map(m -> "  - " + m)
                .collect(Collectors.joining("\n"));
        }
    }

    private static String formatUnexpectedIssues(List<Issue> issues) {
        return formatMessageList(
            "Unexpected issues while parsing",
            issues.stream().map(i -> "[%s] %s".formatted(i.getSeverity(), i.getMessage())).toList()
        );
    }

    public void hasNoIssues() {
        if (!actual.isEmpty()) {
            failWithMessage(formatUnexpectedIssues(actual));
        }
    }

    public void hasNoErrors() {
        if (actual.stream().anyMatch(i -> i.getSeverity() == Severity.ERROR)) {
            failWithMessage(formatUnexpectedIssues(actual));
        }
    }

    public void hasIssues(String... messages) {
        var missingIssues = Arrays.stream(messages)
            .filter(message -> actual.stream().noneMatch(issue -> issue.getMessage().equals(message)))
            .toList();
        var unexpectedIssues = actual.stream()
            .filter(issue -> Arrays.stream(messages).noneMatch(message -> issue.getMessage().equals(message)))
            .toList();

        if (!missingIssues.isEmpty() || !unexpectedIssues.isEmpty()) {
            var messageBuilder = new StringBuilder();
            if (!missingIssues.isEmpty()) {
                messageBuilder.append(formatMessageList("Missing issues while parsing", missingIssues));
            }
            if (!unexpectedIssues.isEmpty()) {
                if (!messageBuilder.isEmpty()) {
                    messageBuilder.append("\n");
                }
                messageBuilder.append(formatUnexpectedIssues(unexpectedIssues));
            }
            failWithMessage(messageBuilder.toString());
        }
    }

}
