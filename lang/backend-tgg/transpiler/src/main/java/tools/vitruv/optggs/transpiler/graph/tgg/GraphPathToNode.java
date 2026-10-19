package tools.vitruv.optggs.transpiler.graph.tgg;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import tools.vitruv.optggs.operators.FQN;

import java.util.ArrayList;

@RequiredArgsConstructor
@AllArgsConstructor
@Getter
public class GraphPathToNode {
    private final FQN root;
    ArrayList<String> linkPath = new ArrayList<>();

    public void addLinkToPath(String reference) {
        linkPath.add(reference);
    }

    public GraphPathToNode pathToSecondLastNode() {
        return new GraphPathToNode(root, new ArrayList<>(linkPath.subList(0, linkPath.size() - 1)));
    }

    public String getLastLink() {
        return linkPath.getLast();
    }
}
