package tools.vitruv.neojoin.parse;

import org.assertj.core.api.AbstractAssert;
import org.eclipse.xtext.diagnostics.Severity;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.validation.Issue;
import tools.vitruv.neojoin.ast.ViewTypeDefinition;
import tools.vitruv.neojoin.common.Pair;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ParseAssertions extends AbstractAssert<ParseAssertions, List<Issue>> {

    private final ViewTypeDefinition viewType;

    protected ParseAssertions(ViewTypeDefinition viewType, List<Issue> actual) {
        super(actual, ParseAssertions.class);
        this.viewType = viewType;
    }

    public static ParseAssertions assertThat(Pair<ViewTypeDefinition, List<Issue>> actual) {
        //noinspection DataFlowIssue - false positive
        return new ParseAssertions(actual.left(), actual.right());
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

    public ParseAssertions hasNoIssues() {
        if (!actual.isEmpty()) {
            failWithMessage(formatUnexpectedIssues(actual));
        }
        return this;
    }

    public ParseAssertions hasNoErrors() {
        if (actual.stream().anyMatch(i -> i.getSeverity() == Severity.ERROR)) {
            failWithMessage(formatUnexpectedIssues(actual));
        }
        return this;
    }

    /**
     * Asserts that an issue with the given severity and message exists. Other issues are not checked.
     */
    public ParseAssertions hasIssueWithSeverity(Severity severity, String message) {
        if (actual.stream().noneMatch(i -> i.getSeverity() == severity && i.getMessage().equals(message))) {
            failWithMessage("Expected %s '%s' but got:%n%s".formatted(
                severity, message, formatUnexpectedIssues(actual)));
        }
        return this;
    }

    /**
     * Asserts that the issues with the given message mark exactly the given parts of the query text (in any order).
     * Other issues are not checked.
     */
    public ParseAssertions hasIssueAt(String message, String... markedTexts) {
        var text = NodeModelUtils.getNode(viewType).getRootNode().getText();
        var actualMarkedTexts = actual.stream()
            .filter(issue -> issue.getMessage().equals(message))
            .map(issue -> text.substring(issue.getOffset(), issue.getOffset() + issue.getLength()))
            .sorted()
            .toList();
        var expectedMarkedTexts = Arrays.stream(markedTexts).sorted().toList();
        if (!actualMarkedTexts.equals(expectedMarkedTexts)) {
            failWithMessage(
                "Expected issue '%s' at %s but found it at %s", message, expectedMarkedTexts, actualMarkedTexts
            );
        }
        return this;
    }

    /**
     * Asserts that the issues have exactly the given messages (any severity, no other issues).
     */
    public ParseAssertions hasExactlyIssues(String... messages) {
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
        return this;
    }

}
