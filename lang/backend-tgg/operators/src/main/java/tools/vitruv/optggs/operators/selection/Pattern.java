package tools.vitruv.optggs.operators.selection;

import tools.vitruv.optggs.operators.FQN;
import tools.vitruv.optggs.operators.FunctionInvocation;
import tools.vitruv.optggs.operators.Tuple;

import java.util.*;

public class Pattern {
    private final List<PatternLink> links;

    private Pattern() {
        this.links = new ArrayList<>();
    }

    private Pattern(PatternLink link) {
        this.links = new ArrayList<>();
        this.links.add(link);
    }

    private Pattern(Collection<PatternLink> links) {
        this.links = new ArrayList<>();
        this.links.addAll(links);
    }

    private Pattern(Pattern pattern, PatternLink link) {
        this.links = new ArrayList<>();
        this.links.addAll(pattern.links);
        this.links.add(link);
    }

    public Collection<PatternLink> links() {
        return Collections.unmodifiableCollection(links);
    }

    public void addLink(PatternLink link) {
        links.add(link);
    }

    public static Pattern from(FQN element) {
        return new Pattern(new From(element));
    }

    public Pattern join(FQN element, Collection<Tuple<String, String>> properties) {
        return new Pattern(this, new Join(element, properties));
    }

    public Pattern join(FQN element, String originProperty, String destinationProperty) {
        return new Pattern(this, new Join(element, originProperty, destinationProperty));
    }

    public Pattern join(FQN element, String property) {
        return new Pattern(this, new Join(element, property));
    }

    public Pattern join(FQN element, FunctionInvocation functionInvocation) {
        return new Pattern(this, new ThetaJoin(element, functionInvocation));
    }

    public Pattern ref(FQN element, String reference) {
        return new Pattern(this, new Ref(element, reference));
    }

    public FQN top() {
        return links.get(0).element();
    }

    public FQN bottom() {
        return links.get(links.size() - 1).element();
    }

    public Pattern topPattern() {
        return Pattern.from(top());
    }

    public Pattern bottomPattern() {
        return Pattern.from(bottom());
    }

    public int length() {
        return links.size();
    }

    public Collection<FQN> elements() {
        return links.stream().map(PatternLink::element).toList();
    }

    public Tuple<Pattern, PatternLink> popBottom() {
        var last = links.get(links.size() - 1);
        var remainder = links.subList(0, links.size() - 1);
        return new Tuple<>(new Pattern(remainder), last);
    }

    public Tuple<Pattern, PatternLink> popTop() {
        var first = links.get(0);
        var remainder = links.subList(1, links.size());
        return new Tuple<>(new Pattern(remainder), first);
    }

    public boolean startsWith(Pattern other) {
        if (this.links.size() < other.links.size()) return false;
        for (var i = 0; i < other.links.size(); ++i) {
            if (!this.links.get(i).equals(other.links.get(i))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return String.join("", links.stream().map(Object::toString).toList());
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Pattern pattern)) return false;
        return Objects.equals(links, pattern.links);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(links);
    }
}