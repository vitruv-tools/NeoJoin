package tools.vitruv.optggs.transpiler.graph.tgg;

import tools.vitruv.optggs.operators.FQN;

import java.util.ArrayList;

public class GraphPathToNode {
    private final FQN root;
    ArrayList<String> linkPath = new ArrayList<>();

    public GraphPathToNode(FQN root) {
        this.root = root;
    }

    public GraphPathToNode(FQN root, ArrayList<String> linkPath) {
        this.root = root;
        this.linkPath = linkPath;
    }

    public FQN getRoot() {
        return root;
    }

    public ArrayList<String> getLinkPath() {
        return linkPath;
    }

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
